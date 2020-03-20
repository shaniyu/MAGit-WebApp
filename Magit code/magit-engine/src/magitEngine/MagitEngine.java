package magitEngine;

import DataStructures.CommitChanges;
import DataStructures.MagitCommitNode;
import DataStructures.Tree;
import DataStructures.TreeNode;
import Exceptions.BranchAlreadyInUseException;
import Exceptions.FileNotInTree;
import Utils.Files.FilesOperations;
import Utils.SHA1;
import magitObjects.*;
import org.apache.commons.io.FileUtils;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;
import xmlObjects.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
// this class the core of magit ("the game")
public class MagitEngine {
    private Repository currentRepo;
    private String userName;
    //CommitChanges openChangesLists;
    private List<String> deletedFiles;
    private List<String> newFiles;
    private List<String> changedFiles;
    private XmlValidation xmlValidator;
    private Dictionary<String, String> commitsSha1ToIdForExport;
//    private Dictionary<String, String> foldersSha1ToIdForExport;
//    private Dictionary<String, String> blobsSha1ToIdForExport;
    private Dictionary<GitObject, String> foldersSha1ToIdForExport;
    private Dictionary<GitObject, String> blobsSha1ToIdForExport;

    // ctor for creating a new repository
    public MagitEngine() {
        //In case the program started and first thing i do is load repository from existing repository
        //it couldn't execute loadRepositoryFromPath because repository was null in the beginning
        //so i created an empty default constructor for Repository and Branch to initialize the repository.
        currentRepo = null;
        userName = "Administrator";
        this.deletedFiles = new ArrayList<>();
        this.newFiles = new ArrayList<>();
        this.changedFiles = new ArrayList<>();
        //openChangesLists = new CommitChanges(); //The constructor initializes the 3 lists
    }
    public List<Branch> getBranches()
    {
        return currentRepo.getAllOfTheBranches();
    }

//    public magitEngine.MagitEngine(MagitRepository magitRepository, String userName) {
//        xmlValidator = new magitEngine.XmlValidation(magitRepository);
//        currentRepo = null;
//        this.userName = userName;
//        deletedFiles = new ArrayList<>();
//        newFiles = new ArrayList<>();
//        changedFiles = new ArrayList<>();
//    } // replaced by setXmlValidator

    public void setXmlValidator(String userName, MagitRepository magitRepository)
    {
        xmlValidator = new XmlValidation(magitRepository);
        currentRepo = null;
        this.userName = userName;
        this.deletedFiles = new ArrayList<>();
        this.newFiles = new ArrayList<>();
        this.changedFiles = new ArrayList<>();
    }

//    public void overrideRepoInLocation(MagitRepository magitRepository) throws IOException, Exception{
//
//        String xmlRepositoryLocation = magitRepository.getLocation();
//
//        if(currentRepo == null){
//            currentRepo = new Repository();
//        }
//        //Clearing the existing repository from the WC, and branches + objects content
//        currentRepo.clearRepository(xmlRepositoryLocation);
//
//        currentRepo.spreadRepositoryFromXml(magitRepository);
//    }

//    public void createAndSpreadRepositoryFromXml(MagitRepository magitRepository) throws Exception{
//
//        File repoFolder = new File(magitRepository.getLocation());
//
//        if(!repoFolder.mkdir()){ //if creating the repository directory fails{
//             throw new IOException("Could not create the folder " + repoFolder.getPath() + "\nCheck your permittions."); }
//
//        if(currentRepo == null){
//            currentRepo = new Repository();
//        }
//
//        currentRepo.spreadRepositoryFromXml(magitRepository);
//    }


    public void pushNewCommitsToRemoteTargetBranch(String targetBranch, HashSet<String> commitsDelta) throws Exception
    {
        // the remote repo of repoName by userName, has already the branch targetBranch, just push to it the new commit
        // that tagetBranch has in the LR repoName by userName

        // should get the current commit of the RTB of remoteBranch and get all the delta from the RTB in out repository to the remoteBranch
        // including zip files of commits, folders, files, branches
        // get the sha1 of the LR target branch
        File targetBranchFileInLocal = new File(currentRepo.getBranchesPath() + File.separator + targetBranch);
        String branchSha1 = FileUtils.readFileToString(targetBranchFileInLocal, StandardCharsets.UTF_8);
        // zip file representing a commit in the remote repository
        File sha1ZipFile = new File(currentRepo.getRemoteRepoPath() + File.separator
                + ".magit" + File.separator
                + "objects" + File.separator
                + branchSha1 + ".zip");
        if (sha1ZipFile.exists()){// we already have this commit in the remote repo
            //Still need to add it to the commits that were changed in the PR (even if the commit itself exists in the repository)
            commitsDelta.add(branchSha1);
            return;
        }
        else
        {
            // we don't have this commit and its files on the remote repo, lets bring it
            reccursiveFetchCommitFromLocalToRemote(branchSha1, commitsDelta);
        }
    }


    public boolean isBranchRTB(String inputBranchName) throws IOException{
        // check if we have a RTB for the branch in the file RTB

        String RTBFilePath = currentRepo.getMagitPath() + File.separator + "RTB";
        ArrayList<String> RTBContent = FilesOperations.readAllContentFromFileByStrings(RTBFilePath);
        for (int i = 0 ; i < RTBContent.size(); i+=3)
        {
            String branchName = RTBContent.get(i);
            if (branchName.equals(inputBranchName))
                return true;

        }
        return false;
    }

    //This method accepts one commit and one commit sha1 (commitToCalculateChanges and commitSha1Original)
    //and calculates the open changes of commitToCalculateChanges relatively to commitSha1Original)
    //It spreads the WC of commitToCalculateChanges in a temp folder in .magit folder, and builds a git object tree from commitSha1Original
    //then calculates the open changes the same way as calculated on the WC with updateAllOpenChangesOfWC
    //This method doesn't check if commitChangesToUpdate is null!!!!!!!!
    // the list that are created contains relative path of each file, starting *after* the fullPathToSpreadTo
    public void calculateChangesBetween2Commits(Commit commitToCalculateChanges, String commitSha1Original, CommitChanges commitChangesToUpdate, String fullPathToSpreadTo, String pathNameToSpread) throws Exception{

        //File tempFolder = new File(currentRepo.getMagitPath() + File.separator + "temp");
        File tempFolder = new File(fullPathToSpreadTo);
        //Creating the temp folder in which we spread the "WC" of commitToCalculateChanges
        spreadCommitToCheckInTemp(tempFolder, commitToCalculateChanges.getSha1());

        //Getting all files of commitToCalculateChanges from temp folder
        Stream<Path> fileWalk = Files.walk(Paths.get(tempFolder.getPath()));
        List<String> allFilePathsInCommitToCalculateChanges = fileWalk.filter(Files::isRegularFile)//Getting all text files in repository folder
                .map(x -> x.toString())
                .collect(Collectors.toList());//Getting list of the files paths

        clearAllChangesOfCommit(commitChangesToUpdate);

        //Calculating gitObjectTree of commitSha1Original
        Tree originalCommitGitObjectTree = currentRepo.initializeGitObjectTreeForCommit(commitSha1Original);

        //Checking changes in files between commitToCalculateChanges and commitSha1Original
        for(String filePathToCheck : allFilePathsInCommitToCalculateChanges){
            try{
                //If the file exists in the tree but it was changed
                if(currentRepo.checkIfFileOrFolderChangedForCommit(filePathToCheck,"", originalCommitGitObjectTree, pathNameToSpread))
                {
                    String relativePath = filePathToCheck.substring(fullPathToSpreadTo.length() +1);
                    commitChangesToUpdate.addToChangedFiles(relativePath);
                }
            }
            // if the file wasn't in the tree, it is new, then we get FileNotInTree exception
            //If the file didn't exist in the tree- it's a new file
            catch (FileNotInTree e){
                String relativePath = filePathToCheck.substring(fullPathToSpreadTo.length() +1);
                commitChangesToUpdate.addToNewFiles(relativePath);
            }
            catch (IOException e){
                throw new IOException("Couldn't read from file " + filePathToCheck + ". Please check your permissions");
            }
        }

        //Getting deleted files between commitToCalculateChanges and commitSha1Original
        if(originalCommitGitObjectTree != null) {
            findAndSetDeletedFilesInGitObjectTree(originalCommitGitObjectTree.getRoot(), commitChangesToUpdate.getDeletedFiles());
        }
    }

    public void spreadCommitToCheckInTemp(File tempFolder, String commitSha1ToCalculateChanges) throws Exception{

        //Initializing the temp folder
        if(tempFolder.exists()) {
            if (!deleteTempFolder(tempFolder)){
                throw new IOException("Could not delete some of the files in the temp folder");
            }
        }
        if(!tempFolder.mkdir()){
            throw new Exception("Couldn't create temp folder in .magit folder");
        }

        //Spreading commitSha1ToCalculateChanges files to temp folder
        currentRepo.putCommitContentOnTempFolder(commitSha1ToCalculateChanges, tempFolder);
    }

    //This function is exactly like FilesOperations.deleteFolder
    //but i needed to know if the deletion succeeded and didn't want to change the existing one
    // (FilesOperations.deleteFolder doesn't return true/false)
    private boolean deleteTempFolder(File folder) {

        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    if (!deleteTempFolder(f)) {
                        return false;
                    }
                } else {
                    if (!f.delete()) {
                        return false;
                    }
                }
            }
        }
        if(!folder.delete()){
            return false;
        }
        return true;
    }

    public void updateAllOpenChangesOfWC() throws Exception {

        Stream<Path> fileWalk = Files.walk(Paths.get(currentRepo.getRepositoryLocation()));
        clearAllOpenChangesOfWC();
        if(currentRepo.getGitObjectTree() != null){
            //Need to reset all git objects' wasChecked flags before checking status
            resetGitObjectTreeWasChecked(currentRepo.getGitObjectTree().getRoot());
        }

        List<String> allFilePathsInWC = fileWalk.filter(Files::isRegularFile) //Getting all text files in repository folder
                .filter(x-> !(x.toAbsolutePath().toString().contains(".magit"))) //Excluding any file paths that contain ".magit"
                    .map(x -> x.toString()).collect(Collectors.toList()); //Getting list of the files paths

        //If there is no commit in the repository then all files in the WC are new
        if(!currentRepo.isThereAnyCommitInTheRepository(currentRepo.getRepositoryLocation())){
            for(String filePathToCheck : allFilePathsInWC){
                newFiles.add(filePathToCheck);
            }
        }
        else{ //If there is at least one commit in the repository then we need to check
            //all new/changes/deleted files relatively to the current commit.
            for(String filePathToCheck : allFilePathsInWC){
                try{
                    //If the file exists in the tree but it was changed
                    if(currentRepo.checkIfFileOrFolderChanged(filePathToCheck,""))
                    {
                        changedFiles.add(filePathToCheck);
                    }
                    // if the file wasn't in the tree, it is new, then we get FileNotInTree exception
                }
                //If the file didn't exist in the tree- it's a new file
                catch (FileNotInTree e){
                    newFiles.add(filePathToCheck);
                }
                catch (IOException e){
                    throw new IOException("Couldn't read from file " + filePathToCheck + ". Please check your permissions");
                }
            }
        }

        if(currentRepo.getGitObjectTree() != null) {
            findAndSetDeletedFilesInGitObjectTree(currentRepo.getGitObjectTree().getRoot(), deletedFiles);
        }
    }

    private void clearAllOpenChangesOfWC(){
        deletedFiles.clear();
        newFiles.clear();
        changedFiles.clear();
    }

    private void clearAllChangesOfCommit(CommitChanges commitChangesToUpdate){
        commitChangesToUpdate.getDeletedFiles().clear();
        commitChangesToUpdate.getNewFiles().clear();
        commitChangesToUpdate.getChangedFiles().clear();
    }

    private void resetGitObjectTreeWasChecked(TreeNode root) {
        if(root == null){
            return;
        }
        else if (root.getmChildren() == null){
            root.getmNode().setWasChecked(false);
        }
        else{
            for(TreeNode child : root.getmChildren()){
                if(child.getmNode().getmFolderType() == FolderType.FOLDER){
                    child.getmNode().setWasChecked(false);
                    resetGitObjectTreeWasChecked(child);
                }
                else{ //child is a file
                    child.getmNode().setWasChecked(false);
                }
            }
        }
    }

    private void findAndSetDeletedFilesInGitObjectTree(TreeNode root, List<String> deletedFiles) {

        if (root == null){
            return;
        }
        else if(root.getmChildren() == null){
            //Since when performing git status we go through all of the files (not directories)
            //in the WC- then the assumption is that if there's a file in the GitObject tree
            //that we haven't proccessed- then it was at some point in the WC (because it's in the tree)
            //but was deleted later on so we won't look for it in the tree. In this case- the wasChecked
            //flag will be false for this gitObject that represents a file, and we want to add it to deletedFiles.
            if(root.getmNode().getmFolderType() == FolderType.FILE && !(root.getmNode().getWasChecked())){
                // Important! we skip the first word, which is the repository directory name, and add only the relayive path od the file in the repo
                // for example : WRONG: /test1/1/2/yuval.txt. CORRECT: 1/2/yuval.txt
                String nameOfFileWithoudRepoName = root.getmNode().getmPath().substring(currentRepo.getRepoFolderName().length() +1);
                deletedFiles.add(nameOfFileWithoudRepoName);
            }
        }
        else{
            for(TreeNode child : root.getmChildren()){
                if (child.getmNode().getmFolderType() == FolderType.FOLDER){
                    findAndSetDeletedFilesInGitObjectTree(child, deletedFiles);
                }
                else{ //if current git object is a file
                    if (!(child.getmNode().getWasChecked())){
                        String nameOfFileWithoudRepoName = child.getmNode().getmPath().substring(currentRepo.getRepoFolderName().length() +1);
                        deletedFiles.add(nameOfFileWithoudRepoName);
                    }
                }
            }
        }
    }

//    private String fullPathFromPartialRepoPath(String partialPath)
//    {
//        int indexWithoutRepoFolder = partialPath.indexOf(File.separator) +1;
//        String fullPathOfObject = getRepositoryLocation() + File.separator + partialPath.substring(indexWithoutRepoFolder);
//        return fullPathOfObject;
//    }


    public static boolean isPathAlreadyARepo(String path) {

        // true <=>  existing folder .magit -> objects & branches
        File repositoryPath = new File(path);
        if (!repositoryPath.exists()) {
            // this path doesn't exist, so it is not a magit repository.
            return false;
        } else {
            // path does exist
            File magitFolder = new File(path + File.separator + ".magit");
            if (!magitFolder.exists())
                return false;
        }
        return true;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRepositoryLocation() {
        if (currentRepo == null) {
            return "N\\A";
        }
        return currentRepo.getRepositoryLocation();
    }

    public String showAllBranches()  throws IOException{
            String allBranchesInfo = "";

            for (Branch branch : currentRepo.getAllOfTheBranches()) {
                allBranchesInfo += getBranchInfo(branch);
                allBranchesInfo += "\n";
            }
            return allBranchesInfo;
    }

    public String showAllBranchesNames()
    {
        String allBranchesNames = ("");
        for (Branch branch : currentRepo.getAllOfTheBranches()) {
            allBranchesNames += branch.getName() + "\n";
        }
        return allBranchesNames;
    }

    public ArrayList<String> getAllBranchesNames()
    {
        ArrayList<String> allBranchesNames = new ArrayList<>();
        for (Branch branch : currentRepo.getAllOfTheBranches())
        {
            allBranchesNames.add(branch.getName());
        }
        return allBranchesNames;
    }

    private String getBranchInfo(Branch branch) throws IOException{
        String informationToShow = null;
        if (branch != null) {
            // branch name + sha1 of its commit
            informationToShow= branch.getName();
            // if there is a commit on the branch ( the only case there isn't, is on new repository)
            if (branch.getCommit() != null && branch.getCommit().getSha1() != null && ! branch.getCommit().getSha1().isEmpty()) {
                String branchSha1 = branch.getCommit().getSha1();
                informationToShow += " : " + branchSha1 + ", ";
                //read the fourth line in the commit zip file, that has the commit message
                informationToShow += (FilesOperations.readAllContentFromZipFileByStrings(currentRepo.getObjectsPath() + File.separator + branchSha1 + ".zip")).get(3);
            }
            // put a sign on the current branch
            if (currentRepo.getHeadBranch().getName().equals(branch.getName())) {
                informationToShow += "   <--- HEAD";
            }
        }
        return(informationToShow);
    }

    public boolean isBranchTheHeadBranch(String branchName)
    {
        return currentRepo.getHeadBranch().getName().equals(branchName);
    }

    public void addNewBranch(String newBranchName) throws BranchAlreadyInUseException, IOException {

        // creating a new branch on the head commit, if there isn't branch with this name
        Branch newBranch = new Branch(newBranchName, currentRepo.getHeadBranch().getCommit());
        if (!currentRepo.getAllOfTheBranches().contains(newBranch)) {
            currentRepo.addNewBranch(newBranch);
            String newBranchPath = currentRepo.getRepositoryLocation() + File.separator + ".magit" + File.separator +
                    "branches" + File.separator + newBranchName;
            File newBranchFile = new File(newBranchPath);
            if ( ! newBranchFile.exists())
            {
                boolean success = newBranchFile.createNewFile();
                if (newBranch.getCommit() != null)
                {
                    FilesOperations.writeTextToFile(newBranch.getCommit().getSha1(), newBranchPath);
                }
            }
        }
        else
            throw new BranchAlreadyInUseException(newBranchName);
    }

    public void addNewBranchOnSpecificCommit(String newBranchName, Commit commit) throws BranchAlreadyInUseException, IOException {

        // creating a new branch on the commit, if there isn't branch with this name
        Branch newBranch = new Branch(newBranchName, commit);
        if (!currentRepo.getAllOfTheBranches().contains(newBranch)) {
            currentRepo.addNewBranchOnSpecificCommit(newBranch, commit);
            String newBranchPath = currentRepo.getRepositoryLocation() + File.separator + ".magit" + File.separator +
                    "branches" + File.separator + newBranchName;
            File newBranchFile = new File(newBranchPath);
            if ( ! newBranchFile.exists())
            {
                boolean success = newBranchFile.createNewFile();
                if (newBranch.getCommit() != null)
                {
                    FilesOperations.writeTextToFile(newBranch.getCommit().getSha1(), newBranchPath);
                }
            }
        }
        else
            throw new BranchAlreadyInUseException(newBranchName);
    }

    public void setCurrentRepo(Repository currentRepo) {
        this.currentRepo = currentRepo;
    }

    public Repository getCurrentRepo() {
        return currentRepo;
    }

    public ArrayList<String> getHistoryOfActiveBranch() throws IOException
    {
        ArrayList<String> history = currentRepo.getHistoryOfActiveBranch();
        return history;
    }

    public List<String> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<String> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public List<String> getNewFiles() {
        return newFiles;
    }

    public void setNewFiles(List<String> newFiles) {
        this.newFiles = newFiles;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<String> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public void checkOutToBranch(String branchNameToCheckout) throws Exception
    {
        currentRepo.checkOutToBranch(branchNameToCheckout);
    }
    public void deleteBranch(String branchToDelete) throws IOException, Exception
    {
        currentRepo.deleteBranch(branchToDelete);
    }

    public String createCommit( String messageFromUser, String secondParentSha1 ) throws Exception
    {
        String newCommitSha1 = null ;

        if (!(currentRepo.isThereAnyCommitInTheRepository(currentRepo.getRepositoryLocation()))){
            //Do first commit for repository
            newCommitSha1 = currentRepo.CreateFirstCommit(userName, messageFromUser);
        }
        else{
            //Do commit for repository, merge commits is only a not-first commit
            newCommitSha1 = currentRepo.CreateCommitNotFirst(userName, messageFromUser, secondParentSha1);
        }
        return newCommitSha1;
    }

    public boolean isThereOpenChangesInRepository() throws Exception
    {
        if (currentRepo.getGitObjectTree() == null)
        {
            Stream<Path> fileWalk = Files.walk(Paths.get(currentRepo.getRepositoryLocation()));
            List<String> allFilePathsInWC = fileWalk.filter(x-> !(x.toAbsolutePath().toString().contains(".magit")))//Excluding any file paths that contain ".magit"
                    .filter(Files::isRegularFile)//Excluding any folders that don't contain regular files (which are empty folders to the system)
                    .map(x -> x.toString()).collect(Collectors.toList()); //Getting list of the files paths
            return !(allFilePathsInWC.size() == 0); // if there are no regular files in the repository then we don't have open changes
        }
        else
        {
            updateAllOpenChangesOfWC();
            // some of the files has been changed somehow, so there are changes
            return (deletedFiles.size() > 0 || newFiles.size() > 0 || changedFiles.size() > 0);
        }
    }
    public void createEmptyRepository(String repoPath, String repoName) throws IOException
    {
        setCurrentRepo(new Repository(repoPath, repoName));
    }

    public String getRepositoryName()
    {
        return currentRepo.getRepositoryName();
    }

    public boolean isACommit(String sha1ToCheck) throws IOException
    {
        //check if the sha1 is one of the commits, by iterating over all the sha1's in .magit/branches/commits file
        ArrayList<String> commitsSha1List =getAllCommitsSha1();
        return commitsSha1List.contains(sha1ToCheck);
    }

    public ArrayList<String> getAllCommitsSha1() throws IOException
    {
        // read all sha1's in commits file
        String commitsFilePath =  currentRepo.getBranchesPath() + File.separator + "commits";
        ArrayList<String> commitsSha1List = FilesOperations.readAllContentFromFileByStrings(commitsFilePath);
        return commitsSha1List;
    }

    public void resetBranchSha1(String sha1ToReset) throws Exception
    {
        currentRepo.resetHeadBranchSha1(sha1ToReset);
    }

    public void continueWithExistingReposotory(String repositoryPath) throws Exception{
        if(currentRepo == null){
            setCurrentRepo(new Repository());
        }
        // set the repository
        currentRepo.loadRepositoryFromPath(repositoryPath);
    }

    public XmlValidation getXmlValidator() {
        return xmlValidator;
    }

    public void exportRepoToXml(String pathOfXmlFileToExport) throws JAXBException, IOException
    {
        MagitRepository thisRepo = buildMagitRepositoryFromCurrentRepo();
        File file = new File(pathOfXmlFileToExport);
        JAXBContext jaxbContext = JAXBContext.newInstance(MagitRepository.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(thisRepo, file);
        jaxbMarshaller.marshal(thisRepo, System.out);
    }

    private MagitRepository buildMagitRepositoryFromCurrentRepo() throws IOException
    {
        commitsSha1ToIdForExport = new Hashtable<>(); // dictionary for sha1-id pairs for commits
        foldersSha1ToIdForExport = new Hashtable<>(); // dictionary for sha1-id pairs for folders
        blobsSha1ToIdForExport = new Hashtable<>();   // dictionary for sha1-id pairs for blobs
        // set all commit id from the commits text file
        String commitsFilePath = currentRepo.getBranchesPath() + File.separator + "commits";
        // list of sha1s
        ArrayList<String> allOfRepositoryCommitsSha1 = FilesOperations.readAllContentFromFileByStrings(commitsFilePath);
        initializeCommitsDictionary(allOfRepositoryCommitsSha1);

        MagitRepository res = new MagitRepository();

        // set name and location
        res.setName(currentRepo.getRepositoryName());
        res.setLocation(currentRepo.getRepositoryLocation());

        //set branches
        MagitBranches repositoryBranches = getMagitBranches();
        res.setMagitBranches(repositoryBranches);

        // set commits, and also set folders and blobs dictionary
        MagitCommits allRepoCommits = getMagitCommits(allOfRepositoryCommitsSha1);
        res.setMagitCommits(allRepoCommits);

        // set folders and blobs  of every commit in the repoistory
        MagitBlobs blobs = new MagitBlobs();
        MagitFolders folders = new MagitFolders();
        setFoldersAndBlobs(allOfRepositoryCommitsSha1, blobs, folders);

        res.setMagitBlobs(blobs);
        res.setMagitFolders(folders);
        return res;
    }

    //
    private void setFoldersAndBlobs(ArrayList<String> allOfRepositoryCommits, MagitBlobs blobs, MagitFolders folders)
            throws IOException
    {
        for ( String sha1OfCommit : allOfRepositoryCommits)
        {
            // for each commit. update blobs and folders objects
            String commitPath = getObjectsPath() + File.separator + sha1OfCommit + ".zip";

            ArrayList<String > commitDetails = FilesOperations.readAllContentFromZipFileByStrings(commitPath);
            String currCommitRootFolderSha1 = commitDetails.get(0);
            // add the root folder itself to folders
            MagitSingleFolder rootFolder = new MagitSingleFolder();
            rootFolder.setIsRoot(true);
            rootFolder.setName("");
            rootFolder.setLastUpdateDate(commitDetails.get(4));
            rootFolder.setLastUpdater(commitDetails.get(5));

            // get the root folder id , using only name,sha1 and type of GitObject
            GitObject folderObject = new GitObject("",null, currCommitRootFolderSha1, FolderType.FOLDER, null, null);
            rootFolder.setId(foldersSha1ToIdForExport.get(folderObject));

            // add all the items of the root folder to its items
            addItemsToFolder(currCommitRootFolderSha1, rootFolder);

            // root folder is now initialized, add it to folders
            // no need to check existence, because root folders must be uniqe
            folders.addFolderToFolders(rootFolder);
            // call recursive to any other folders and blobs
            recursiveSetFoldersAndBlobsOfRepo(currCommitRootFolderSha1, blobs, folders);
        }
    }

    // add values to blobs and folders Magit objects
    private void recursiveSetFoldersAndBlobsOfRepo(String folderSha1, MagitBlobs blobs, MagitFolders folders) throws IOException
    {
        // gets a sha1 of folder, and recursively changing blobs and folders
        String folderSha1FilePath= getObjectsPath() + File.separator + folderSha1 + ".zip";
        File folderDescriptionFile = new File(folderSha1FilePath);
        // list all of the obejcts in this dir
        ArrayList<GitObject> objectsOfCurrentDir = Repository.getAllObjectsOfDirectory(folderDescriptionFile);

        for ( GitObject object : objectsOfCurrentDir)
        {
            if (object.getmFolderType().equals(FolderType.FILE))
            {
                // this object is a file, we don't need a recursive call
                MagitBlob blob = new MagitBlob();
                String blobId = blobsSha1ToIdForExport.get(object);

                // perform this part only if this blob wasn't added yet to blobs
                if ( ! blobs.isBlobInTheList(blobId))
                {
                    blob.setId(blobId);
                    blob.setName(object.getmName());
                    blob.setLastUpdater(object.getmLastModifiedBy());
                    blob.setLastUpdateDate(object.getmLastModifiedDate());
                    // set content
                    String fileContentPath = getObjectsPath() + File.separator + object.getmSHA1() + ".zip";
                    blob.setContent(FilesOperations.readAllContentFromZipFile(fileContentPath)); // read from file
                    // add blob to the list only if it is not already in ( might happen by another commit)
                    blobs.addBlobToBlobs(blob);
                }
            }
            else if (object.getmFolderType().equals(FolderType.FOLDER))
            {
                MagitSingleFolder folder = new MagitSingleFolder();
                String folderId = foldersSha1ToIdForExport.get(object);

                // perform this part only if this folder wasn't added yet to folders
                if (! folders.isFolderInTheList(folderId))
                {
                    folder.setId(folderId);
                    folder.setName(object.getmName());
                    folder.setLastUpdater(object.getmLastModifiedBy());
                    folder.setLastUpdateDate(object.getmLastModifiedDate());
                    folder.setIsRoot(false);

                    // initialize items of this folder
                    addItemsToFolder(object.getmSHA1() , folder);
                    folders.addFolderToFolders(folder);
                    //  call recursively on this folder
                    recursiveSetFoldersAndBlobsOfRepo(object.getmSHA1(), blobs, folders);
                }
            }
        }
    }

    // this method add to the magit single folder argument, all of its items, based on the list of GitObject it has
    private void addItemsToFolder( String folderSha1, MagitSingleFolder folder) throws IOException
    {
        // the file of the folder description
        File currFolderFile = new File(getObjectsPath() + File.separator + folderSha1 + ".zip");
        ArrayList<GitObject> objectsList = Repository.getAllObjectsOfDirectory(currFolderFile); // might throws IOException

        // iterate over this folder objects
        for (GitObject objectOfFolder : objectsList)
        {
            // add contained item ( folder\blob) to the current folder items list
            Item item = new Item();
            if (objectOfFolder.getmFolderType().equals(FolderType.FILE)) {
                item.setType("blob");
                item.setId(blobsSha1ToIdForExport.get(objectOfFolder));
            }
            else {
                item.setType("folder");
                item.setId(foldersSha1ToIdForExport.get(objectOfFolder));
            }
            folder.addItemToItems(item);
        }
    }
    // also sets folders and blobs dictionaries
    private MagitCommits getMagitCommits(ArrayList<String> allOfRepositoryCommits) throws IOException
    {
        MagitCommits res = new MagitCommits();
        for ( String commitSha1 : allOfRepositoryCommits) // iterate over commits
        {
            MagitSingleCommit currCommit = new MagitSingleCommit();

            currCommit.setId(commitsSha1ToIdForExport.get(commitSha1)); // set commit id by dictionary
            String commitPath = getObjectsPath() + File.separator + commitSha1 + ".zip";
            ArrayList<String > commitDetails = FilesOperations.readAllContentFromZipFileByStrings(commitPath);
            String commitMsg;
            String commitAuthor;
            String dateOfCreating;
            // initialize root folder id
            String sha1OfRoot = commitDetails.get(0);
            // initialize preceding commits
            String firstParentSha1 = commitDetails.get(1);
            String secondParentSha1 = commitDetails.get(2);
            // initializ simple fields
            commitMsg = commitDetails.get(3);
            dateOfCreating = commitDetails.get(4);
            commitAuthor = commitDetails.get(5);

            currCommit.setMessage(commitMsg);
            currCommit.setDateOfCreation(dateOfCreating);
            currCommit.setAuthor(commitAuthor);

            //initialize preceding commits
            PrecedingCommits parentsOfThisCommit = new PrecedingCommits();
            if (!firstParentSha1.equals("null"))
            {
                String idOfCommitFirstParent = commitsSha1ToIdForExport.get(firstParentSha1);
                parentsOfThisCommit.addPrecedingCommit(idOfCommitFirstParent);
            }
            if (!secondParentSha1.equals("null"))
            {
                String idOfCommitSecondParent = commitsSha1ToIdForExport.get(secondParentSha1);
                parentsOfThisCommit.addPrecedingCommit(idOfCommitSecondParent);
            }
            currCommit.setPrecedingCommits(parentsOfThisCommit);
            // initialize root folder id of current commit, and also all of the blobs and folders of this commit
            //adding root folder to foldersSha1ToIdForExport dictionary
            GitObject rootFolderObject = new GitObject("",
                    null,
                    commitDetails.get(0),
                    FolderType.FOLDER,
                    dateOfCreating,
                    commitAuthor);
            foldersSha1ToIdForExport.put(rootFolderObject, Integer.toString(foldersSha1ToIdForExport.size()+1));
            initializeFoldersAndBlobsDictionary(commitDetails.get(0));
            // set id of root folder, by using the folders dictionary
            currCommit.getRootFolder().setId(foldersSha1ToIdForExport.get(rootFolderObject));

            // add to the list of res
            res.addCommitToList(currCommit);
        }
        return res;
    }

    private String getObjectsPath()
    {
        return currentRepo.getObjectsPath();
    }

    private void initializeCommitsDictionary(ArrayList<String> allCommitsSha1)
    {
        // add all sha1-id pairs to dictionary
        int id = 1;
        for (String sha1 : allCommitsSha1)
        {
            commitsSha1ToIdForExport.put(sha1, Integer.toString(id));
            id++;
        }
    }

    // recursive function to initialize blobs and folders dictionaries
    // first called with the root folder sha1
    private void initializeFoldersAndBlobsDictionary(String folderSha1) throws IOException
    {
        String workingCopyFilePAth = getObjectsPath() + File.separator + folderSha1 + ".zip";
        File workingCopyDescriptionFile = new File(workingCopyFilePAth); // the path to the file that describes the WC

        // get all the objects in the root folder of the current commit and add to the dictionaries
        ArrayList<GitObject> objectsOfCurrentRoot = Repository.getAllObjectsOfDirectory(workingCopyDescriptionFile);
        for ( GitObject object : objectsOfCurrentRoot)
        {
            if (object.getmFolderType().equals(FolderType.FOLDER))
            {
                if (foldersSha1ToIdForExport.get(object) == null)
                {
                    // key not in the hashmap - put it now
                    // insert key with id value that depends on the dictionary size, starting from 1
                    String newId = Integer.toString(foldersSha1ToIdForExport.size()+1);
                    foldersSha1ToIdForExport.put(object, newId );
                    // recursive call to this function with the current folder sha1
                    initializeFoldersAndBlobsDictionary(object.getmSHA1());
                }
            }
            else if (object.getmFolderType().equals(FolderType.FILE))
            {
                if (blobsSha1ToIdForExport.get(object) == null )
                {
                    // key not in the hashmap - put it now
                    // insert key with id value that depends on the dictionary size, starting from 1
                    String newId = Integer.toString(blobsSha1ToIdForExport.size() +1);
                    blobsSha1ToIdForExport.put(object, newId);
                }
            }
        }
    }
    private MagitBranches getMagitBranches()
    {
        // not relevant for exercise 2, only bonus at ex 1
        MagitBranches res = new MagitBranches();
        ArrayList<MagitSingleBranch> singleBranches = new ArrayList<>();
        res.setHead(currentRepo.getHeadBranch().getName()); // initialize head name
        // initialize all the single branches
        for (Branch branch : currentRepo.getAllOfTheBranches())
        {
            MagitSingleBranch currBranch = new MagitSingleBranch();
            currBranch.setName(branch.getName()); // set name
            // set id of pointed commit
            if (branch.getCommit() != null)
            {
                currBranch.getPointedCommit().setId(commitsSha1ToIdForExport.get(branch.getCommit().getSha1()));
            }
            else
            {
                currBranch.getPointedCommit().setId("");
            }
            // not tracking and not remote
            currBranch.setIsRemote(false);
            currBranch.setTracking(false);
            singleBranches.add(currBranch);
        }
        res.setMagitSingleBranch(singleBranches);
        return res;
    }



    //This method accepts 2 commit sha1s and finds the most recent common ancestor of them
    //It uses Aviad's AncestorFinder library
    public Commit getMostRecentCommonAncestor(String firstCommitSha1, String secondCommitSha1)throws Exception{

        //AncestorFinder accepts a function that accepts SHA1 (String) and its return value is CommitRepresentative
        //which is an interface that is defined in AncestorFinder library
        //Our Commit class implements this interface
        AncestorFinder ancestorFinder = new AncestorFinder(this::mapSha1ToCommitRepresentative);
        String ancestorCommitSha1 = ancestorFinder.traceAncestor(firstCommitSha1, secondCommitSha1);

        //returning the commit object of the most recent common ancestor
        return currentRepo.getCommitTree().getSha1ToNodesDictionary().get(ancestorCommitSha1).getCommit();
    }

    //The function sent to AncestorFinder
    public CommitRepresentative mapSha1ToCommitRepresentative(String commitSHA1){ //throws Exception{

        //Find the MagitCommitNode by sha1 in the dictionary, and return the commit held in is which implements CommitRepresentative
        MagitCommitNode magitCommitNode = currentRepo.getCommitTree().getSha1ToNodesDictionary().get(commitSHA1);
        return magitCommitNode.getCommit();
    }
    public Dictionary<String, MagitCommitNode> getTreeNodesDictinary()
    {
        return currentRepo.getCommitTree().getSha1ToNodesDictionary();
    }

    // we can assume the sha1 of the two commits are different
    public Boolean mergeIsFastForward(String sha1OfCommit1, String sha1OfCommit2, String sha1OfAncenstor)
    {
        return (sha1OfCommit1.equals(sha1OfAncenstor) || sha1OfCommit2.equals(sha1OfAncenstor));
    }


    // this method initialize headCommitChangesToAncenstor and theirsCommitChangesToAncenstor with
    // the differences between each branch top to the ancenstor commit, the result is a union of the 6 lists
    // the method also spread ansenstor commit to a ancenstor folder under .magit folder
    public HashSet<String> createListOfChangedFilesInTwoBranches(Commit commitOfBRanchToMerge, String ancenstorSha1, CommitChanges headCommitChangesToAncenstor, CommitChanges theirsCommitChangesToAncenstor) throws Exception
    {
        // compare "theirs" with ancenstor
        calculateChangesBetween2Commits(commitOfBRanchToMerge , ancenstorSha1, theirsCommitChangesToAncenstor, getCurrentRepo().getMagitPath() + File.separator + "theirs", "theirs");
        calculateChangesBetween2Commits(getCurrentRepo().getHeadBranch().getCommit(), ancenstorSha1, headCommitChangesToAncenstor, getCurrentRepo().getMagitPath() + File.separator + "head", "head");

        // spread the ancenstor commit to a path in magit folder
        File pathToSpreadAncenstor = new File(getCurrentRepo().getMagitPath() + File.separator + "ancenstor");
        if (pathToSpreadAncenstor.exists())
        {
            // delete this temp folder from last time
            FilesOperations.deleteFolder(pathToSpreadAncenstor);
        }
        pathToSpreadAncenstor.mkdirs(); // create the folder
        getCurrentRepo().putCommitContentOnTempFolder(ancenstorSha1, pathToSpreadAncenstor);

        // unite all the lists of any kind of changes to one big list, from both ancenstor-head, ancenstor-theirs calculations
        HashSet<String> filesThatChangesAndMightCauseConflicts = new HashSet<>();
        filesThatChangesAndMightCauseConflicts.addAll(headCommitChangesToAncenstor.getDeletedFiles());
        filesThatChangesAndMightCauseConflicts.addAll(headCommitChangesToAncenstor.getChangedFiles());
        filesThatChangesAndMightCauseConflicts.addAll(headCommitChangesToAncenstor.getNewFiles());
        filesThatChangesAndMightCauseConflicts.addAll(theirsCommitChangesToAncenstor.getDeletedFiles());
        filesThatChangesAndMightCauseConflicts.addAll(theirsCommitChangesToAncenstor.getChangedFiles());
        filesThatChangesAndMightCauseConflicts.addAll(theirsCommitChangesToAncenstor.getNewFiles());

        // return the union
        return filesThatChangesAndMightCauseConflicts;
    }

    public ArrayList<String> calculateConflictedFilesListAndHandleNonConflictedFiles(HashSet<String> filesThatChangesAndMightCauseConflicts, CommitChanges headCommitChangesToAncenstor,
                                            CommitChanges theirsCommitChangesToAncenstor, String branchToMergeWith) throws Exception {
        ArrayList<String> filesWithConflicts = new ArrayList<>();

        // iterate all the files in filesThatChangesAndMightCauseConflicts and define the match case of it - conflict or not
        for (String filePath : filesThatChangesAndMightCauseConflicts) {
            if (isFileHasConflict(filePath, headCommitChangesToAncenstor, theirsCommitChangesToAncenstor)) {
                filesWithConflicts.add(filePath);
            } else {
                // this file was changed, but this changes doesn't make a conflict
                // no conflict on this file, take the file from where it was changed - theirs or head, and put on WC
                // the file is the the list of files that was changed, so one of the commits must have changed it
                putUpdatedFileOnWc(filePath, headCommitChangesToAncenstor, theirsCommitChangesToAncenstor);

                // ENGINE CODE
                // if a file wasn't changed at all from ancenstor to both commits, it won't be in the hasSet
                // and it is already in the WC so no need to update it
            }
        }
        return filesWithConflicts;
    }

    private Boolean isFileHasConflict(String filePath, CommitChanges headCommitChangesToAncenstor, CommitChanges theirsCommitChangesToAncenstor) throws Exception
    {
        // find the match case to each file that has changed somehow by one of the branches
        // use xor to tell if the file was changed *only* in one branch, so it is not a conflict
        if (headCommitChangesToAncenstor.isFileInTheListsUnion(filePath) ^ theirsCommitChangesToAncenstor.isFileInTheListsUnion(filePath))
        {
            return false;
        }
        else if(headCommitChangesToAncenstor.getDeletedFiles().contains(filePath) && theirsCommitChangesToAncenstor.getDeletedFiles().contains(filePath))
        {
            return false;
            // because both of the branches deleted this file in some point, this is not a conflict
        }
        else if ((headCommitChangesToAncenstor.getNewFiles().contains(filePath) ||
                headCommitChangesToAncenstor.getChangedFiles().contains(filePath))
                && ( theirsCommitChangesToAncenstor.getNewFiles().contains(filePath) ||
                theirsCommitChangesToAncenstor.getChangedFiles().contains(filePath)))
        {
            String pathOfFileInHeadDir = getRepositoryLocation() + File.separator + ".magit" + File.separator + "head" + File.separator + filePath;
            String pathOfFileInTheirsDir = getRepositoryLocation() + File.separator + ".magit" + File.separator + "theirs" + File.separator + filePath;
            String sha1OfFileInHead = SHA1.createSha1(new File(pathOfFileInHeadDir));
            String sha1OfFileInTheirs = SHA1.createSha1(new File(pathOfFileInTheirsDir));
            if (sha1OfFileInHead.equals(sha1OfFileInTheirs))
            {
                return false; // the file has the same sha1 in both branches, this is the same file after all the changed.
            }
            else
                return true;
        }
        else
            return true;
    }


    // this method gets a file that was changed in one of the branches, and doesn't have a conflict
    // just need to determine which branch changed it, and use the changed file on WC
    private void putUpdatedFileOnWc(String filePath, CommitChanges headCommitChangesToAncenstor, CommitChanges theirsCommitChangesToAncenstor) throws Exception
    {
        // we know for sure this file was changed only by one of the branches
        if (headCommitChangesToAncenstor.isFileInTheListsUnion(filePath))
        {
            // this file was changed by the head branch only, it is already on the WC in it final version
            // nothing to do
        }
        else // changed by "theirs"
        {
            // this file was changed by "theirs" branch
            // put what was on "theirs" branch to the WC
            if (theirsCommitChangesToAncenstor.getDeletedFiles().contains(filePath))
            {
                // the change is - "theirs" deleted the file. need to delete it on WC ( head didn't change it)
                File fileToDelete = new File( getRepositoryLocation() + File.separator + filePath);
                if (fileToDelete.exists())
                {
                    if ( fileToDelete.delete() == false)
                    {
                        throw new Exception("Can't delete the file " + fileToDelete.getPath());
                    }
                }
            }
            else
            {
                // changed or added by "theirs", so copy this file from there to the WC
                // there might be also new folders to include this file, that were added
                // the new file path on the WC
                String filePathOnWC = getRepositoryLocation() + File.separator + filePath;
                File changedFile = new File(filePathOnWC);
                if (changedFile.exists())
                {
                    changedFile.delete();
                }
                // copy the file from theirs to head
                // if folder of this file doesn't exist, create it
                if (! changedFile.getParentFile().exists())
                {
                    changedFile.getParentFile().mkdirs();
                }
                changedFile.createNewFile();
                String theirsFile = getRepositoryLocation() + File.separator + ".magit" +
                        File.separator + "theirs" + File.separator + filePath;
                String theirsFileContent = FileUtils.readFileToString(new File(theirsFile), StandardCharsets.UTF_8);
                FilesOperations.writeTextToFile(theirsFileContent, filePathOnWC);
            }
        }
    }

    public void handleFastForwardMerge(String sha1OfHead, String sha1OfTheirs, String sha1OfAncenstor) throws  Exception {
        if (sha1OfHead.equals(sha1OfAncenstor)) {
            getCurrentRepo().resetHeadBranchSha1(sha1OfTheirs);
        } else {
            // do nothing, the other branch commits are already included in the head branch
        }
    }

    public Repository clone(String pathOfRepoToCloneFrom, String pathOfNewRepo, String newDir, String newRepoName, String remoteRepoUser) throws Exception
    {
        String repoPath = pathOfNewRepo + File.separator + newDir;
        // first copy the directory to the new repository location
        copyRemoteRepoToLocal(pathOfRepoToCloneFrom, pathOfNewRepo + File.separator + newDir );

        // fix new repo name file to the newRepoName string insread of the remote name
        String nameFilePath = repoPath + File.separator + ".magit" + File.separator + "name";

        String remoteRepoName = FileUtils.readFileToString(new File(nameFilePath), StandardCharsets.UTF_8);
        FilesOperations.writeTextToFile(newRepoName, nameFilePath);

        // load the new cloned repository to our system
        if(getCurrentRepo() == null){
            Repository newRepo = new Repository();
            newRepo.setRemoteRepoUsername(remoteRepoUser);
            setCurrentRepo(newRepo);
        }
        currentRepo.setRemoteRepoPath(pathOfRepoToCloneFrom);
        currentRepo.setRepositoryLogicalNameForUser(newRepoName);
        currentRepo.writeRemoteNameAndPathToFile(pathOfRepoToCloneFrom, repoPath, remoteRepoName);
        // create files for every remote branch, in clone every local branch has a remote branch
        currentRepo.cretaeRemoteBranchesFilesAndDeleteLocalBranchesThatAreNotHead(repoPath, remoteRepoName);
        // create the RTB file and write the head (local and remote couplt) branch to it.
        currentRepo.createHeadRemoteTrackingBranchFile(repoPath, remoteRepoName);

        // in this step, the file system has all the new files that are relevant for the new repo
        currentRepo.loadRepositoryFromPath(pathOfNewRepo + File.separator + newDir);

        removeUnwantedFolders();
        return currentRepo;
    }



    private void copyRemoteRepoToLocal(String repoToClonePath, String whereToClonePath) throws IOException
    {
        try
        {
            if (!new File(whereToClonePath).exists())
            {
                new File(whereToClonePath).mkdirs();
            }

            File srcDir = new File(repoToClonePath);
            File destDir = new File(whereToClonePath);
            FileUtils.copyDirectory(srcDir, destDir);
        }
        catch (IOException e)
        {
            throw new IOException("Could not copy " + repoToClonePath + " to " + whereToClonePath);
        }
    }


    //This method gets a commit Sha1 and returns an ArrayList of all the remote branches pointing to that commit
    public ArrayList<String> getAllRemoteBranchesOnCommit(String commitSha1)
    {
        ArrayList<String> remoteBranchesOnCommit = new ArrayList<>();

        for (Branch b: currentRepo.getAllOfTheBranches())
        {
            if (b.getIsRemote() && b.getCommit().getSha1().equals(commitSha1))
            {
                remoteBranchesOnCommit.add(b.getName());
            }
        }

        return remoteBranchesOnCommit;
    }

    public ArrayList<String> getAllBranchesOnCommit(String commitSha1){

        ArrayList<String> branchesOnCommit = new ArrayList<>();

        for (Branch b: currentRepo.getAllOfTheBranches())
        {
            if (b.getCommit().getSha1().equals(commitSha1))
            {
                branchesOnCommit.add(b.getName());
            }
        }

        return branchesOnCommit;
    }

    public void addRTBranch(String remoteBranchToTrack) throws Exception
    {
        // this metod gets a remote branch name ( full name, for example: repo1/shani)
        // and add a new RT Branch with the same name ( but without the remote name, for example: shani)
        // that follows the RB
        // cut the name of the RB without the repo name
        String newBranchName = remoteBranchToTrack.substring(remoteBranchToTrack.lastIndexOf(File.separator) +1);
        if ( ! getAllBranchesNames().contains(newBranchName))
            addNewBranch(newBranchName);
        addRTBToRTBFile(newBranchName);
    }

    private void addRTBToRTBFile(String remoteBranchToTrack) throws IOException
    {
        String remoteTrackingBranchesFilePath = getRepositoryLocation() + File.separator + ".magit" + File.separator +"RTB";
        String stringToAppend = remoteBranchToTrack + System.lineSeparator()
                                + currentRepo.getRemoteRepoName() + File.separator +
                                remoteBranchToTrack + System.lineSeparator() + ";"
                                + System.lineSeparator();

        FilesOperations.appendTextToFile(stringToAppend, remoteTrackingBranchesFilePath);
    }

    private void removeUnwantedFolders()
    {
        // in case we cloned from a repo that has the "head" "theirs" and "ancenstor" directories, we delete them
        String headFolderPath = currentRepo.getMagitPath() + File.separator + "head";
        String theirsFolderPath = currentRepo.getMagitPath() + File.separator + "theirs";
        String ancenstorFolderPath = currentRepo.getMagitPath() + File.separator + "ancenstor";

        File headFolder = new File(headFolderPath);
        File theirsFolder = new File(theirsFolderPath);
        File ancenstorFolder = new File(ancenstorFolderPath);

        if (headFolder.exists())
        {
            FilesOperations.deleteFolder(headFolder);
        }
        if (theirsFolder.exists())
        {
            FilesOperations.deleteFolder(theirsFolder);
        }
        if (ancenstorFolder.exists())
        {
            FilesOperations.deleteFolder(ancenstorFolder);
        }
    }

    public String addRTBForRBAndCheckoutToIt(String branchNameToCheckout) throws Exception
    {
        // branchNameToCheckout is a remote branch name (for example : repo1/yuvalBranch
        // we checkout to the RB, and then add a new RTB
        // finally we checkout to it
        checkOutToBranch(branchNameToCheckout);
        addRTBranch(branchNameToCheckout);
        String newBranchName = branchNameToCheckout.substring(branchNameToCheckout.lastIndexOf(File.separator)+1);
        checkOutToBranch(newBranchName);

        return newBranchName;
    }

    
    public ArrayList<Branch> getAllRemoteBranches()
    {
        ArrayList<Branch> remoteBranches = new ArrayList<>();

        for (Branch b: currentRepo.getAllOfTheBranches())
        {
            if (b.getIsRemote())
            {
                remoteBranches.add(b);
            }
        }

        return remoteBranches;
    }

    // this method get a full name of a branch ( for RB: repo1/yuvalBranch for example)
    private boolean isBrnachInTheRemoteBranches(String branchName)
    {
        for (Branch b: currentRepo.getAllOfTheBranches())
        {
            if (b.getIsRemote() && b.getName().equals(branchName))
                return true;
        }
        return false;
    }

    public Boolean isRepoHasARemote()
    {
        return (currentRepo.getRemoteRepoName() != null && currentRepo.getRemoteRepoPath() != null);
    }

    public void fetch() throws IOException
    {
        // first we fetch all the existing remote branches, and update them
        ArrayList<Branch> remoteBranches = getAllRemoteBranches();
        for (Branch b: remoteBranches)
        {
            String branchName = b.getName().substring(b.getName().lastIndexOf(File.separator) +1);
            File remoteBranchFileInRemote = new File(currentRepo.getRemoteRepoPath() +
                                            File.separator + ".magit" + File.separator+
                                            "branches" + File.separator + branchName);

            // get the remote sha1 of the branch and update the branch in this repo
            String branchSha1InRemote = FileUtils.readFileToString(remoteBranchFileInRemote, StandardCharsets.UTF_8);
            b.setCommit(new Commit(branchSha1InRemote));
            // write the file to the RB in this repo
            String remoteBranchPathInThisRepo = currentRepo.getBranchesPath()+File.separator + b.getName();
            FilesOperations.writeTextToFile(branchSha1InRemote, remoteBranchPathInThisRepo);
            fetchRemoteBranch(b);
        }

        // and now we also get new branches from the remote and add to this repos RB
        getAllNewRemoteBranchesFromRemoteRepo();

        currentRepo.initializeMagitCommitTree();

    }

    private void getAllNewRemoteBranchesFromRemoteRepo() throws IOException
    {
        // this method iterates all over the branches in the remote repository
        // and add each new branch in the remote, to be a RB in this repo
        String remoteBranchesPathInRemote = currentRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator + "branches";

        // all the files in the remote repo, under branches folder
        File[] branchesInRemote = new File (remoteBranchesPathInRemote).listFiles();

        for (File f : branchesInRemote)
        {
            if (! f.isDirectory() && !f.getName().equals("HEAD") &&
                    !f.getName().equals("commits"))
            {
                String remoteBranchName = currentRepo.getRemoteRepoName() + File.separator + f.getName();
                // If this file is a local branch in the *remote* repository, that we don't have a RB for-
                // then create a new RB for it
                if (! isBrnachInTheRemoteBranches(remoteBranchName))
                {
                    createAndfetchRemoteBranchToThisRepo(remoteBranchName);
                }
            }
        }
    }

    // this method gets a full name of a RB that isn't in this repo yet
    // need to create its file and also bring all the data of it overtime
    public void createAndfetchRemoteBranchToThisRepo(String remoteBranchName) throws IOException
    {
        // first, creat a new file for this RB in under .magit/branches/remoteName
        Branch newRB = createNewRBInThisRepo(remoteBranchName);

        // second, we want to bring all the data of the new RB - commits, files & folders to objects
        fetchRemoteBranch(newRB);
    }

    // updates file system and also the branches list
    // this method create a new file for a RB in the .magit/branches/remoteName folder, and write the sha1 to it
    // this method returns the new RB
    private Branch createNewRBInThisRepo(String remoteBranchName) throws IOException
    {
        String branchInRemotePath = currentRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator + "branches";
        String newRBPath = currentRepo.getBranchesPath() ;

        // create new file for new RB
        File newRBFile = new File(newRBPath + File.separator + remoteBranchName);
        if (! newRBFile.exists())
            newRBFile.createNewFile();

        // read SHA1 from the branch in the remote
        String branchName = remoteBranchName.substring(remoteBranchName.lastIndexOf(File.separator)+1);
        File branchFileInRemote = new File(branchInRemotePath + File.separator + branchName);
        String sha1OfBranchInRemote = FileUtils.readFileToString(branchFileInRemote, StandardCharsets.UTF_8);
        // write this SHA1 to the new RB
        FilesOperations.writeTextToFile(sha1OfBranchInRemote, newRBPath+ File.separator + remoteBranchName);

        // add the new RB to the branches list
        Branch newBranch = new Branch(remoteBranchName, new Commit(sha1OfBranchInRemote));
        newBranch.setIsRemote(true);
        currentRepo.addNewBranch(newBranch);
        return newBranch;
    }

    private void fetchRemoteBranch(Branch b) throws IOException
    {
        // should get the current commit of the RB of b and get all the delta from the branch in the remote to the branch b
        // including zip files of commits, folders, files, branches
        String branchSha1 = b.getCommit().getSha1(); // we can assume there is a sha1, cause collaborations doesn't work with rmpty repo
        // zip file representing a commit
        File sha1ZipFile = new File(currentRepo.getObjectsPath() + File.separator + branchSha1 + ".zip");
        if (sha1ZipFile.exists()) // we already have this commit in our repo
            return;
        else
        {
            // we don't have this commit and its files on our repo, lets bring it
            reccursiveFetchCommitFromRemote(b.getCommit().getSha1());
        }
    }

    // is called with non-null value only
    // copy commit and all its zip files from the remote
    // copy the zip files to the objects path and also add to the commit file the new SHA1
    private void reccursiveFetchCommitFromRemote(String remoteSha1OfACommit) throws IOException
    {
        String commitZipPathInRemote = currentRepo.getRemoteRepoPath() + File.separator +
                ".magit" + File.separator + "objects" + File.separator + remoteSha1OfACommit + ".zip";

        String commitZipFileInThisRepo = currentRepo.getObjectsPath() + File.separator +
                                    remoteSha1OfACommit + ".zip";

        // first copy the commit zip file from the romte to local
        File commitInThisRepo = new File(commitZipFileInThisRepo);
        File commitInRemote = new File(commitZipPathInRemote);
        if (! commitInThisRepo.exists())
        {
            // we dont have this commit, copy its zip file
            FileUtils.copyFile(commitInRemote, commitInThisRepo);
            // add the new commit SHA1 to the commits file
            currentRepo.writeSha1ToCommitsFile(remoteSha1OfACommit);

            ArrayList<String> commitInfo = FilesOperations.readAllContentFromZipFileByStrings(commitZipFileInThisRepo);
            String firstParentSha1 = commitInfo.get(1);
            String secondParentSha1 = commitInfo.get(2);
            File firstParentCommitZipInThisRepo = new File(currentRepo.getObjectsPath() + File.separator + firstParentSha1 + ".zip");
            File secondParentCommitZipInThisRepo = new File(currentRepo.getObjectsPath() + File.separator + secondParentSha1 + ".zip");

            if (! firstParentSha1.equals("null") && !firstParentCommitZipInThisRepo.exists())
            {
                // need to call function with first parent
                reccursiveFetchCommitFromRemote(firstParentSha1);
            }
            if (! secondParentSha1.equals("null") && !secondParentCommitZipInThisRepo.exists())
            {
                // need to call function with second parent
                reccursiveFetchCommitFromRemote(secondParentSha1);
            }

            // do anyway, also for commit with no parents
            String rootFolderSha1 = commitInfo.get(0);
            File rootFolderZipFile = new File(currentRepo.getObjectsPath() +
                    File.separator + rootFolderSha1 + ".zip");
            if (! rootFolderZipFile.exists())
            {
                // we don't have a zip file of the root folder of this commit, recurssive add it and all its content files
                recurssiveFetchFolderFromRemote(rootFolderSha1);
            }
        }
    }

    // this method is called with a folderSha1 we don't have on this repo, but have on the remote
    private void recurssiveFetchFolderFromRemote(String folderSha1) throws IOException
    {
        File folderZipFileInThisRepo = new File(currentRepo.getObjectsPath() + File.separator + folderSha1 + ".zip");
        File folderZipInRemote = new File(currentRepo.getRemoteRepoPath() + File.separator
                                            + ".magit" + File.separator + "objects" + File.separator +
                                                    folderSha1 + ".zip");

        // first copy the folder zip file to this repo
        FileUtils.copyFile(folderZipInRemote, folderZipFileInThisRepo);

        ArrayList<GitObject> foldersObjects = currentRepo.getAllObjectsOfDirectory(folderZipFileInThisRepo);
        for (GitObject object : foldersObjects)
        {
            String newObjectPath = currentRepo.getObjectsPath() + File.separator + object.getmSHA1() + ".zip";
            File newObjectFile = new File(newObjectPath);

            if (object.getmFolderType().equals(FolderType.FOLDER))
            {
                // the current object is folder, if we don't have its sha1 in the local repo, call recurssivly
                if (!newObjectFile.exists())
                {
                    recurssiveFetchFolderFromRemote(object.getmSHA1());
                }
            }
            else
            {
                // the current object is a file, copy if we don't have it
                if (! newObjectFile.exists())
                {
                    // just copy from the remote
                    File objectInRemote = new File(currentRepo.getRemoteRepoPath()
                                                    + File.separator + ".magit" + File.separator + "objects" +
                                                        File.separator + object.getmSHA1() +  ".zip");
                    FileUtils.copyFile(objectInRemote, newObjectFile);
                }
            }
        }
    }

    // check if we have a RTB for the head branch in the file RTB
    public Boolean checkHeadIsRTB() throws IOException
    {
        String headName = currentRepo.getHeadBranch().getName();
        String RTBFilePath = currentRepo.getMagitPath() + File.separator + "RTB";

        ArrayList<String> RTBContent = FilesOperations.readAllContentFromFileByStrings(RTBFilePath);
        for (int i = 0 ; i < RTBContent.size(); i+=3)
        {
            String branchName = RTBContent.get(i);
            if (branchName.equals(headName))
                return true;

        }
        return false;
    }

    public void pull() throws Exception
    {
        // fetching only the RB that head branch is tracking after
        File remoteBranchFileInRemote = new File(currentRepo.getRemoteRepoPath() +
                File.separator + ".magit" + File.separator +
                "branches" + File.separator + currentRepo.getHeadBranch().getName());

        // get the remote sha1 of the branch and update the branch in this repo
        String branchSha1InRemote = FileUtils.readFileToString(remoteBranchFileInRemote, StandardCharsets.UTF_8);
        String remoteBranchName = currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHeadBranch().getName();
        Branch b = currentRepo.getBranchByName(remoteBranchName);
        b.setCommit(new Commit(branchSha1InRemote));

        //Writing the sha1 of the commit in the remote branch in RR to the remote branch in this repo (LR)
        String remoteBranchPathInThisRepo = currentRepo.getBranchesPath()+File.separator + b.getName();
        FilesOperations.writeTextToFile(branchSha1InRemote, remoteBranchPathInThisRepo);
        // fetching the remote branch that fits the head (which is a RTB)
        fetchRemoteBranch(b);

        // in this exercise we won't have conflicts in pull
        // in this exercise the RTB is the ancenstor of the RB commit
        handleFastForwardMerge(currentRepo.getHeadBranch().getCommit().getSha1(),
                                b.getCommit().getSha1(), currentRepo.getHeadBranch().getCommit().getSha1());
    }


    public boolean isRemoteBranchAndBranchInRemoteAreOnSameCommit() throws IOException{

        //path of branch file in remote repository
        String branchInRemotePath = currentRepo.getRemoteRepoPath() + File.separator + ".magit" +
                File.separator + "branches" + File.separator + currentRepo.getHeadBranch().getName();
        //path of remote branch file in local repository
        String remoteBranchName = currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHeadBranch().getName();
        String remoteBranchInLocalPath = currentRepo.getBranchesPath() + File.separator + remoteBranchName;

        //reading the commit pointed by the branch in remote that is tracked by the head branch in this repo
        String commitSha1PointedByBranchInRemote = FileUtils.readFileToString(new File(branchInRemotePath), StandardCharsets.UTF_8);
        //reading the commit pointed by the remote branch in the local repo
        String commitSha1PointedByRemoteBranchInLocal = FileUtils.readFileToString(new File(remoteBranchInLocalPath), StandardCharsets.UTF_8);
        return(commitSha1PointedByRemoteBranchInLocal.equals(commitSha1PointedByBranchInRemote));
    }

    public boolean isThereOpenChangesOnRemoteRepo() throws Exception{

        //We will load the remote repository later to check if there are open changes-
        // so we need to save the current repository location in order to get bach to it
        String currentLocalRepoPath = currentRepo.getRepositoryLocation();

        //We need to load the remote repository in order to check if there are any open changes there
        currentRepo.loadRepositoryFromPath(currentRepo.getRemoteRepoPath());
        boolean isThereOpenChangesInRemoteRepository = isThereOpenChangesInRepository();

        //loading back the current local repository
        currentRepo.loadRepositoryFromPath(currentLocalRepoPath);

        return isThereOpenChangesInRemoteRepository;
    }

    public void push(boolean isBranchToPushRTB) throws Exception
    {
        // IMPLEMENT!
        String branchName = currentRepo.getHeadBranch().getName();
        String remoteBranchName = currentRepo.getRemoteRepoName() + File.separator + branchName;
        String branchesInRemotePath = currentRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator + "branches";
        // fetching only the RTB in the LR head that tracks the branch in the RR
        File rtbBranchFileInLocal = new File(currentRepo.getBranchesPath() + File.separator + branchName);

        // get the sha1 of the LR rtb in head and update the branch in the RR, and the RB in the LR
        String branchSha1InLocalHeadRTB = FileUtils.readFileToString(rtbBranchFileInLocal, StandardCharsets.UTF_8);

        File branchInRemote = new File ( branchesInRemotePath + File.separator + branchName);

        File remoteBranchInLocal = new File(currentRepo.getRepositoryLocation() +
                File.separator + ".magit" + File.separator +
                "branches" + File.separator + remoteBranchName);

        //Writing branch sha1 to branch in RR and to remote branch in LR
        //The only case in which the branch file in remote/ remote branch file in local doesn't exist-
        // is if we're pushing a new branch to RR (Bonus 4)
        if(!branchInRemote.exists()){
            branchInRemote.createNewFile();
        }

        FileUtils.writeStringToFile(branchInRemote, branchSha1InLocalHeadRTB, StandardCharsets.UTF_8);

        if(!remoteBranchInLocal.exists()){
         remoteBranchInLocal.createNewFile();
        }
        FileUtils.writeStringToFile(remoteBranchInLocal, branchSha1InLocalHeadRTB, StandardCharsets.UTF_8);

        //If the branch to push isn't RTB- then we want to add it to the RTB file in LR (Bonus 4)
        Branch remoteBranch = null;
        if(!isBranchToPushRTB){
            String rtbFileInLRPath = currentRepo.getMagitPath() + File.separator + "RTB";
            String strForRTB = branchName + System.lineSeparator()
                    + remoteBranchName + System.lineSeparator() +
                    ";" + System.lineSeparator();
            // append the branch to RTB file
            FilesOperations.appendTextToFile(strForRTB, rtbFileInLRPath);
            //Adding the new remote branch to the allOfTheBranches list
            remoteBranch = new Branch(remoteBranchName, new Commit(branchSha1InLocalHeadRTB));
            remoteBranch.setIsRemote(true);
            currentRepo.getAllOfTheBranches().add(remoteBranch);
        }
        else {
            //Setting the remote branch in this repository with the new RTB sha1
            remoteBranch = currentRepo.getBranchByName(remoteBranchName);
            remoteBranch.setCommit(new Commit(branchSha1InLocalHeadRTB));
        }

        // fetching the RTB branch that fits branch in RR
        fetchRTBBranchToRR(remoteBranch);

        //Need to spread new commit content on WC in the remote repo only if the head in the remote is the remote branch!!!
        File headFileInRemote = new File (branchesInRemotePath + File.separator + "HEAD");
        String headBranchInRemote = FileUtils.readFileToString(headFileInRemote, StandardCharsets.UTF_8);
        if(headBranchInRemote.equals(branchName)){
            putCommitContentOnWorkingCopyOfRemote(branchSha1InLocalHeadRTB);
        }

        currentRepo.resetAndUpdateAllOfTheBranches();  // update branches list
        currentRepo.initializeMagitCommitTree();

    }

    public void fetchRTBBranchToRR(Branch remoteBranch) throws IOException{

        // should get the current commit of the RTB of remoteBranch and get all the delta from the RTB in out repository to the remoteBranch
        // including zip files of commits, folders, files, branches
        String branchSha1 = currentRepo.getHeadBranch().getCommit().getSha1(); // we can assume there is a sha1, cause collaborations doesn't work with empty repo
        // zip file representing a commit in the remote repository
        File sha1ZipFile = new File(currentRepo.getRemoteRepoPath() + File.separator
                + ".magit" + File.separator
                + "objects" + File.separator
                + branchSha1 + ".zip");
        if (sha1ZipFile.exists()) // we already have this commit in the remote repo
            return;
        else
        {
            // we don't have this commit and its files on the remote repo, lets bring it
            reccursiveFetchCommitFromLocalToRemote(branchSha1, null);
        }
    }

    // is called with non-null value only
    // copy commit and all its zip files from local to remote
    // copy the zip files to the objects path and also add to the commit file the new SHA1
    public void reccursiveFetchCommitFromLocalToRemote(String rtbSha1OfACommit, HashSet<String> commitsDelta) throws IOException
    {
        String remoteRepoObjectsPath = currentRepo.getRemoteRepoPath() + File.separator
                + ".magit" + File.separator
                + "objects";

        String commitZipPathInLocalRepo = currentRepo.getRepositoryLocation() + File.separator +
                ".magit" + File.separator + "objects" + File.separator + rtbSha1OfACommit + ".zip";

        String commitZipFileInRemoteRepo = remoteRepoObjectsPath + File.separator + rtbSha1OfACommit + ".zip";

        // first copy the commit zip file from the local repo to remote repo
        File commitInThisRepo = new File(commitZipPathInLocalRepo);
        File commitInRemote = new File(commitZipFileInRemoteRepo);

        //Will be used for calculating the commits delta for each PR
        commitsDelta.add(rtbSha1OfACommit);

        if (! commitInRemote.exists())
        {
            // the remote doesn't have this commit, copy the local repo's zip file
            FileUtils.copyFile(commitInThisRepo, commitInRemote);
            // add the new commit SHA1 to the commits file in the remote repository
            writeSha1ToCommitsFileInRemoteRepo(rtbSha1OfACommit);

            ArrayList<String> commitInfo = FilesOperations.readAllContentFromZipFileByStrings(commitZipFileInRemoteRepo);
            String firstParentSha1 = commitInfo.get(1);
            String secondParentSha1 = commitInfo.get(2);
            File firstParentCommitZipInThisRepo = new File(remoteRepoObjectsPath + File.separator + firstParentSha1 + ".zip");
            File secondParentCommitZipInThisRepo = new File(remoteRepoObjectsPath + File.separator + secondParentSha1 + ".zip");

            if (! firstParentSha1.equals("null") && !firstParentCommitZipInThisRepo.exists())
            {
                // need to call function with first parent
                reccursiveFetchCommitFromLocalToRemote(firstParentSha1, commitsDelta);
            }
            if (! secondParentSha1.equals("null") && !secondParentCommitZipInThisRepo.exists())
            {
                // need to call function with second parent
                reccursiveFetchCommitFromLocalToRemote(secondParentSha1, commitsDelta);
            }

            // do anyway, also for commit with no parents
            String rootFolderSha1 = commitInfo.get(0);
            File rootFolderZipFile = new File(remoteRepoObjectsPath +
                    File.separator + rootFolderSha1 + ".zip");
            if (! rootFolderZipFile.exists())
            {
                // we don't have a zip file of the root folder of this commit, recurssive add it and all its content files
                recurssiveFetchFolderFromLocalToRemote(rootFolderSha1);
            }
        }
    }

    // this method is called with a folderSha1 we don't have on the remote repo, but have on this repo
    public void recurssiveFetchFolderFromLocalToRemote(String folderSha1) throws IOException
    {
        String remoteRepoObjectsPath = currentRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator + "objects";
        File folderZipFileInThisRepo = new File(currentRepo.getObjectsPath() + File.separator + folderSha1 + ".zip");
        File folderZipInRemote = new File(remoteRepoObjectsPath + File.separator + folderSha1 + ".zip");

        // first copy the folder zip file from this repo to the remote repo
        FileUtils.copyFile(folderZipFileInThisRepo, folderZipInRemote);

        ArrayList<GitObject> foldersObjects = currentRepo.getAllObjectsOfDirectory(folderZipInRemote);
        for (GitObject object : foldersObjects)
        {
            String newObjectPath = remoteRepoObjectsPath + File.separator + object.getmSHA1() + ".zip";
            File newObjectFile = new File(newObjectPath);

            if (object.getmFolderType().equals(FolderType.FOLDER))
            {
                // the current object is folder, if we don't have its sha1 in the remote repo, call recurssivly
                if (!newObjectFile.exists())
                {
                    recurssiveFetchFolderFromLocalToRemote(object.getmSHA1());
                }
            }
            else
            {
                // the current object is a file, copy if we don't have it
                if (! newObjectFile.exists())
                {
                    // just copy to the remote
                    File objectInLocal = new File(currentRepo.getObjectsPath()
                            + File.separator + object.getmSHA1() +  ".zip");
                    FileUtils.copyFile(objectInLocal, newObjectFile);
                }
            }
        }
    }

    public void writeSha1ToCommitsFileInRemoteRepo(String commitSha1) throws IOException
    {
        String commitsFilePath = currentRepo.getRemoteRepoPath()
                + File.separator + ".magit"
                + File.separator + "branches"
                + File.separator + "commits";
        File commitsFile = new File(commitsFilePath);
        if (!commitsFile.exists())
        {
            commitsFile.createNewFile();
        }
        FilesOperations.appendTextToFile(commitSha1 + "\n", commitsFilePath);
    }

    public void putCommitContentOnWorkingCopyOfRemote(String commitSha1) throws IOException
    {
        // the file that describes the commit
        String remoteRepoObjectsPath =currentRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator + "objects";
        String commitFilePath = remoteRepoObjectsPath + File.separator + commitSha1 + ".zip";
        String workingCopySha1;

        try
        {
            // read first line, where the root folder sha1 is
            ArrayList<String> stringsOfCommitFile = FilesOperations.readAllContentFromZipFileByStrings(commitFilePath);
            workingCopySha1 = stringsOfCommitFile.get(0);
            // first must delete all the working copy files and directories, except the folder .magit
            if (! currentRepo.deleteWorkingCopy(currentRepo.getRemoteRepoPath()))
                throw new IOException("Could not delete some of the files in the remote working copy.");
            // read from the given working copy zip file all the root objects
            File workingCopyDescriptionFile = new File(remoteRepoObjectsPath + File.separator + workingCopySha1 + ".zip"); // the path to the file that describes the WC
            ArrayList<GitObject> filesOfRoot = currentRepo.getAllObjectsOfDirectory(workingCopyDescriptionFile);

            // extract each git object content to the working tree recursively, starting from the repository path
            for (GitObject obj : filesOfRoot)
            {
                currentRepo.putContentOfGitObjectToDir(obj, currentRepo.getRemoteRepoPath());
            }
        }
        // might be thrown by readStringFromFileUntilEnter or getAllObjectsOfDirectory
        catch (IOException e)
        {
            throw e;
        }
    }

    public HashSet<String> getAllSha1InHistoryOfCommit(Commit commitToListHistory) throws IOException
    {
        HashSet<String> res = new HashSet<>();
        res.add(commitToListHistory.getSha1());
        recursiveAddParentCommitsToSet(commitToListHistory, res);
        return res;
    }
    private void recursiveAddParentCommitsToSet(Commit commitToListHistory, HashSet<String> commitsInHistory) throws IOException
    {
        if (!commitToListHistory.getFirstParentSha1().equals("null"))
        {
            commitsInHistory.add(commitToListHistory.getFirstParentSha1());
            recursiveAddParentCommitsToSet(currentRepo.getCommitBySHA1(commitToListHistory.getFirstParentSha1()), commitsInHistory);
        }
        if (! commitToListHistory.getSecondParentSha1().equals("null"))
        {
            commitsInHistory.add(commitToListHistory.getSecondParentSha1());
            recursiveAddParentCommitsToSet(currentRepo.getCommitBySHA1(commitToListHistory.getSecondParentSha1()), commitsInHistory);
        }
    }

    // returns null on non exsiting file
    public String getFileContent(String filePath) throws IOException
    {
        File f  = new File(filePath);
        if (!f.exists())
            return null;

        return FilesOperations.readFileContent(new File(filePath));
    }

    public void approvePullRequest(String targetBranchName, String baseBranchName) throws Exception
    {
        // in pull request in our exercise we can assume that base is the ancenstor of target
        // so this approval will always be a FF-merge, so we can use reset

        Branch targetBranch  = currentRepo.getBranchByName(targetBranchName);
        Branch baseBranch  = currentRepo.getBranchByName(baseBranchName);

        String sha1OfTarget = targetBranch.getCommit().getSha1();
        if(baseBranch.getName().equals(currentRepo.getHeadBranch().getName()))
        {
            // the base is the head, reset the head to the new commit, of the target branch

            // reset head and also put on WC
            currentRepo.resetHeadBranchSha1(sha1OfTarget);
        }
        else
        {
            // target is not the head
            currentRepo.resetBranchThatIsNotHead(baseBranchName, sha1OfTarget);
        }

    }

    public boolean isThereCommitsToUpdate(PullRequest pullRequestToUpdate)
    {
        Repository remoteRepo = pullRequestToUpdate.getRemoteRepository();
        Repository localRepo = currentRepo;

        // calculate the name of the target branch in the RR (without the repo name + '/'
        int indexOfLastSlashInTargetBranchName  = pullRequestToUpdate.getTargetBranchName().lastIndexOf(File.separator);
        String targetBranchNameInRR = pullRequestToUpdate.getTargetBranchName().substring(indexOfLastSlashInTargetBranchName+ 1);

        Branch remoteTargetBranch = remoteRepo.getBranchByName(targetBranchNameInRR);
        Branch localTargetBranch = localRepo.getBranchByName(targetBranchNameInRR);

        return (! remoteTargetBranch.getCommit().getSha1().equals(localTargetBranch.getCommit().getSha1()));
    }

    public void deleteRTBFromFile(String branchToDelete) throws IOException
    {
        String pathOfRTBFile = currentRepo.getMagitPath() + File.separator + "RTB";
        ArrayList<String> linesInRTBFile = FilesOperations.readAllContentFromFileByStrings(pathOfRTBFile);
        // empty the file
        FileUtils.writeStringToFile(new File(pathOfRTBFile), "", StandardCharsets.UTF_8);
        for (int i = 0 ; i < linesInRTBFile.size(); i++)
        {
            if (linesInRTBFile.get(i).equals(branchToDelete))
            {
                i += 2;
            }
            else
            {
                FilesOperations.appendTextToFile(linesInRTBFile.get(i) + System.lineSeparator(), pathOfRTBFile);
            }
        }
    }
}
