package magitObjects;

import DataStructures.MagitCommitNode;
import DataStructures.MagitCommitTree;
import DataStructures.Tree;
import DataStructures.TreeNode;
import Exceptions.*;
import Utils.Compressor;
import Utils.Files.FilesOperations;
import Utils.SHA1;
import magitEngine.Constants;
import xmlObjects.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Repository {
    private Branch headBranch;
    private ArrayList<Branch> allOfTheBranches;
    private String repositoryLocation;
    private String repositoryName;
    private String repositoryLogicalNameForUser;
    private String magitPath;
    private String repoNamePath;
    private String objectsPath;
    private String branchesPath;
    private String headFilePath;
    private String remoteRepoPath = null;
    private String remoteRepoName = null;
    private String remoteRepoUsername = null;

    //Commit ids in the xml are numbered 1-n. Whenever we calculate a commit SHA1-
    //we add the SHA1 (value) with the commit id (key) to this dictionary
    //in order to determine the parent commit SHA1s so we can write them to the commit files
    private Dictionary<String, String> commitFromXmlDictionary;
    private static final char delimiterOfMagitObject = ';';
    private Tree gitObjectTree;
    private MagitCommitTree commitTree;


    // for existing repository or initialize a directory to be an empty magit repository
    public Repository(String repositoryLocation, String repoName) throws IOException {
        try {
            initializeEmptyRepository(repositoryLocation, repoName);
            //Create commit tree
            initializeMagitCommitTree();

        } catch (IOException e) {
            throw e;
        }
    }

    public Repository() {
        headBranch = new Branch();
        allOfTheBranches = new ArrayList<>();
    }

    public void clearRepository(String repositoryPath) {

        File repositoryFile = new File(repositoryPath);
        FilesOperations.deleteFolder(repositoryFile);
    }

    public void spreadRepositoryFromXml(MagitRepository magitRepository, String username) throws Exception{

        //Creating empty repository in this location
        String newRepoPath = Constants.MAGIT_PATH + File.separator + username + File.separator + magitRepository.getName();
        initializeEmptyRepository(newRepoPath, magitRepository.getName());


        if (isRemoteMagitRepo(magitRepository))
        {
            // it means the repo that represented in xml, is a clone of a remote repo
            initializeRemoteFileAndRemotePath(magitRepository, magitRepository.getMagitRemoteReference().getName());
        }

        //Spreading all objects files and branches files- and then loading the repository to the system as if
        //we're loading an existing repository (option 3 in the menu)
        spreadRepositoryFromXmlToObjects(magitRepository);
        //Adding previous commits can be done only after calculating all commit sha1s, so we add them after spreading all files in objects
        addPreviousCommitToEachCommitFile(magitRepository.getMagitCommits());
        spreadRepositoryFromXmlToBranches(magitRepository);
        loadRepositoryFromPath(newRepoPath);
        //In case we loaded an empty repository- the headBranch commit is null so we don't want to spread the working copy
        if(!(headBranch.getCommit() == null)){
            putCommitContentOnWorkingCopy(headBranch.getCommit().getSha1());
        }
    }

    public void spreadRepositoryFromXmlToBranches(MagitRepository magitRepository) throws IOException {

        MagitBranches magitBranches = magitRepository.getMagitBranches();
        MagitCommits magitCommits = magitRepository.getMagitCommits();
        String remoteTrackingBranchesFilePath = null;
        String branchesPath = null;
        String remoteDirPath = null;
        branchesPath = repositoryLocation + File.separator + ".magit" + File.separator + "branches";

        if ( isRemoteMagitRepo(magitRepository))
        {
            // create the RTB file
            remoteTrackingBranchesFilePath = repositoryLocation + File.separator + ".magit" + File.separator +"RTB";
            createRTBFile(remoteTrackingBranchesFilePath);

            // create the RB directory in branches
            remoteDirPath = branchesPath + File.separator + magitRepository.getMagitRemoteReference().getName();
            //remoteDirPath.replace(System.lineSeparator(), "");
            createRBDirectory(remoteDirPath);
        }


        //Creating HEAD file
        String headFileContent = magitBranches.getHead();
        String headFilePath = branchesPath + File.separator + "HEAD";
        File headFile = new File(headFilePath);
        headFile.createNewFile();
        FilesOperations.writeTextToFile(headFileContent, headFile.getPath());

        //Creating all other branched
        for (MagitSingleBranch magitSingleBranch : magitBranches.getMagitSingleBranch()) {
            if (magitSingleBranch.isIsRemote())
            {
                // the new file for the remote branch
                String branchFilePath = branchesPath + File.separator + magitSingleBranch.getName();
                File branchFile = new File(branchFilePath);
                branchFile.createNewFile();
                String localBranchCommitId = magitSingleBranch.getPointedCommit().getId();
                String localBranchSha1  = commitFromXmlDictionary.get(localBranchCommitId);
                FilesOperations.writeTextToFile(localBranchSha1, branchFilePath);
            }
            else
            {
                // this is a local branch
                String commitSha1PointedByBranch = commitFromXmlDictionary.get(magitSingleBranch.getPointedCommit().getId());
                String currBranchFilePath = branchesPath + File.separator + magitSingleBranch.getName();
                File currBranchFile = new File(currBranchFilePath);
                currBranchFile.createNewFile();
                FilesOperations.writeTextToFile(commitSha1PointedByBranch, currBranchFile.getPath());
                if (magitSingleBranch.isTracking())
                {
                    // should add this rtb to the RTB file
                    String branchName = magitSingleBranch.getName();
                    String strForRTB = branchName + System.lineSeparator()
                            + magitSingleBranch.getTrackingAfter()
                            + System.lineSeparator() + ";" + System.lineSeparator();
                    // append the branch to RTB file
                    FilesOperations.appendTextToFile(strForRTB, remoteTrackingBranchesFilePath);
                }
            }
        }

        //Creating "commits" file which contains all SHA1s for all commits in the repository
        String commitFileContent = "";
        String commitFilePath = branchesPath + File.separator + "commits";
        File commitsFile = new File(commitFilePath);
        commitsFile.createNewFile();
        for (MagitSingleCommit magitSingleCommit : magitCommits.getMagitSingleCommit()) {
            commitFileContent += commitFromXmlDictionary.get(magitSingleCommit.getId()) + System.lineSeparator();
        }

        FilesOperations.writeTextToFile(commitFileContent, commitsFile.getPath());
    }

    public void spreadRepositoryFromXmlToObjects(MagitRepository magitRepository) throws IOException, Exception {

        commitFromXmlDictionary = new Hashtable<>();
        MagitFolders magitFolders = magitRepository.getMagitFolders();
        MagitCommits magitCommits = magitRepository.getMagitCommits();

        for (MagitSingleCommit magitSingleCommit : magitCommits.getMagitSingleCommit()) {
            String content = "";
            String folderSha1 = "";
            String rootFolderId = magitSingleCommit.getRootFolder().getId();
            //Find folder in magit folders
            for (MagitSingleFolder magitSingleFolder : magitFolders.getMagitSingleFolder()) {
                if (magitSingleFolder.getId().equals(rootFolderId)) {
                    for (Item item : magitSingleFolder.getItems().getItem()) {
                        content += createFolderContentAndZipFiles(magitRepository, item);
                    }
                    //Calculating the details of the main folder of commit
                    folderSha1 = DigestUtils.sha1Hex(content);
                    String filePathToCreateFolder = objectsPath + File.separator + folderSha1 + ".zip";
                    Compressor.createZipFileFromContent(content, filePathToCreateFolder, folderSha1);
                    //Creating the commit file details
                    String commitDetails = folderSha1 + System.lineSeparator(); //root folder sha1
                    commitDetails += magitSingleCommit.getMessage() + System.lineSeparator();
                    commitDetails += magitSingleCommit.getDateOfCreation() + System.lineSeparator();
                    commitDetails += magitSingleCommit.getAuthor() + System.lineSeparator();
                    String currCommitSha1 = DigestUtils.sha1Hex(commitDetails);
                    commitFromXmlDictionary.put(magitSingleCommit.getId(), currCommitSha1);
                    String filePathToCreateCommit = objectsPath + File.separator + currCommitSha1;
                    File commitFile = new File(filePathToCreateCommit);
                    //Currently creating a regular text file and not a zip file because later-
                    //after we finish calculating all SHA1s we'll add to the commit files their preceding commits
                    commitFile.createNewFile();
                    FilesOperations.writeTextToFile(commitDetails, commitFile.getPath());
                    break;
                }
            }
        }
    }

    public void addPreviousCommitToEachCommitFile(MagitCommits magitCommits) throws Exception{


        for (MagitSingleCommit magitSingleCommit : magitCommits.getMagitSingleCommit()) {
            String prevCommitsToAddToCommitFile = "";
            if (magitSingleCommit.getPrecedingCommits() == null) {
                prevCommitsToAddToCommitFile += "null" + System.lineSeparator() + "null" + System.lineSeparator();
            } else {
                int i = 0;
                for (PrecedingCommits.PrecedingCommit precedingCommit : magitSingleCommit.getPrecedingCommits().getPrecedingCommit()) {
                    String prevCommitSHA1 = commitFromXmlDictionary.get(precedingCommit.getId());
                    prevCommitsToAddToCommitFile += prevCommitSHA1 + System.lineSeparator();
                    i++;
                }
                //Completing preceding commits
                for(int k = i ; k < 2 ; k++){
                    prevCommitsToAddToCommitFile += "null" + System.lineSeparator();
                }
            }
            //Adding the previous commit strings to the commit zip file
            //Reading the content of the zip file -> adding the previous commits from the second row
            //deleting the previous zip file -> zipping the new content with the current commit SHA1 name
            String currCommitSHA1 = commitFromXmlDictionary.get(magitSingleCommit.getId());
            String commitFileToReadPath = objectsPath + File.separator + currCommitSHA1;
            File commitFileToRead = new File(commitFileToReadPath);
            String[] commitContentSeparatedByNewline = FileUtils.readFileToString(commitFileToRead, StandardCharsets.UTF_8).split(System.lineSeparator());
            String newCommitContent = commitContentSeparatedByNewline[0] + System.lineSeparator() +
                    prevCommitsToAddToCommitFile;
            for(int j = 1 ; j < commitContentSeparatedByNewline.length ; j++){
                newCommitContent += commitContentSeparatedByNewline[j] + System.lineSeparator();
            }

            File commitFileToDelete = new File(commitFileToReadPath);
            boolean test = commitFileToDelete.delete();
            Compressor.createZipFileFromContent(newCommitContent, commitFileToReadPath + ".zip", currCommitSHA1);
        }
    }

    private String createFolderContentAndZipFiles(MagitRepository magitRepository, Item item) throws IOException {
        String folderContent = "";
        String mainFolderDetails = "";

        if (item.getType().equals("blob")) {
            for (MagitBlob magitBlob : magitRepository.getMagitBlobs().getMagitBlob()) {
                if (item.getId().equals(magitBlob.getId())) {
                    String blobContent = magitBlob.getContent().trim();
                    String blobSha1 = DigestUtils.sha1Hex(blobContent);
                    String filePathToCreateBlob = objectsPath + File.separator + blobSha1 + ".zip";
                    //Creating zip file for blob
                    if(!(new File(filePathToCreateBlob).exists())){
                        Compressor.createZipFileFromContent(blobContent, filePathToCreateBlob, blobSha1);
                    }
                    //Creating blob details to put in the main folder (root folder of commit) details later (in the calling method)
                    String content = magitBlob.getName() + System.lineSeparator();
                    content += blobSha1 + System.lineSeparator();
                    content += "file" + System.lineSeparator();
                    content += magitBlob.getLastUpdateDate() + System.lineSeparator();
                    content += magitBlob.getLastUpdater() + System.lineSeparator();
                    content += "" + delimiterOfMagitObject + System.lineSeparator();
                    return content;
                }
            }
        } else { //item is a folder
            for (MagitSingleFolder magitSingleFolder : magitRepository.getMagitFolders().getMagitSingleFolder()) {
                if (item.getId().equals(magitSingleFolder.getId())) {
                    for (Item item1 : magitSingleFolder.getItems().getItem()) {
                        folderContent += createFolderContentAndZipFiles(magitRepository, item1);
                    }
                    String folderSha1 = DigestUtils.sha1Hex(folderContent);
                    String filePathToCreateFolder = objectsPath + File.separator + folderSha1 + ".zip";
                    //Creating zip file for folder
                    if(!(new File(filePathToCreateFolder).exists())) {
                        Compressor.createZipFileFromContent(folderContent, filePathToCreateFolder, folderSha1);
                    }
                    //Creating folder details to put in the main folder (root folder of commit) details later (in the calling method)
                    mainFolderDetails += magitSingleFolder.getName() + System.lineSeparator();
                    mainFolderDetails += folderSha1 + System.lineSeparator();
                    mainFolderDetails += "folder" + System.lineSeparator();
                    mainFolderDetails += magitSingleFolder.getLastUpdateDate() + System.lineSeparator();
                    mainFolderDetails += magitSingleFolder.getLastUpdater() + System.lineSeparator();
                    mainFolderDetails += "" + delimiterOfMagitObject + System.lineSeparator();
                    break;
                }
            }
        }
        return mainFolderDetails;
    }

    // return the sha1 of the created commit, can assume there are changes to commit
    // method is called after we know there are open changes
    public String CreateFirstCommit(String userName, String messageFromUser) throws Exception
    {
        File repositoryRoot = new File(this.repositoryLocation); // the root folder of the repo
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS"); // formating the date
        String dateOfNow = format.format(now);
        String commitMessage = messageFromUser;
        String firstParentSha1 = null; // the first commit dosn't have parents
        String secondParentSha1 = null; // the first commit dosn't have parents
        String workingCopySha1 = calculateSha1OfDirectoryFirstCommit(repositoryRoot, userName, dateOfNow);
        String commitSHA1 = writeCommitToFile(workingCopySha1, firstParentSha1, secondParentSha1, commitMessage, dateOfNow, userName);
        //Updating head branch with new commit
        setHeadBranchFileWithCommit(commitSHA1);
        setHeadBranchCommit();
        initializeGitObjectTree(headBranch.getCommit() == null ? null : headBranch.getCommit().getSha1());
        setAllBranchesFileWithNewCommit(commitSHA1); // set all the branches to point to the first commit- this is a must!
        resetAndUpdateAllOfTheBranches(); // updates the branches list
        writeSha1ToCommitsFile(commitSHA1); // append the new sha1 to the commits file
        initializeMagitCommitTree();
        return commitSHA1;
    }

    public void writeSha1ToCommitsFile(String commitSha1) throws IOException
    {
        String commitsFilePath = branchesPath + File.separator + "commits";
        File commitsFile = new File(commitsFilePath);
        if (!commitsFile.exists())
        {
            commitsFile.createNewFile();
        }
        FilesOperations.appendTextToFile(commitSha1 + "\n", commitsFilePath);
    }

    // create a commit that is not the first one, can assume there are changes to commit
    // method is called after we know there are open changes
    public String CreateCommitNotFirst(String userName, String messageFromUser, String secondParentSha1) throws Exception
    {
        String oldSha1 = headBranch.getCommit().getSha1();

        File repositoryRoot = new File(this.repositoryLocation); // the root folder of the repo
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS"); // formating the date
        String dateOfNow = format.format(now);
        String commitMessage = messageFromUser;
        String firstParentSha1 = headBranch.getCommit().getSha1(); // the new commit parent it the current commit

        // we already know the sha1 of the WC has changed
        String workingCopySha1 = calculateSha1OfDirectoryNotFirst(repositoryRoot, userName, dateOfNow);
        String commitSHA1 = writeCommitToFile(workingCopySha1, firstParentSha1, secondParentSha1, commitMessage, dateOfNow, userName);
        // update the head file with new commit
        setHeadBranchFileWithCommit(commitSHA1);
        //Updating head branch with new commit
        setHeadBranchCommit();
        initializeGitObjectTree(headBranch.getCommit() == null ? null : headBranch.getCommit().getSha1());
        resetAndUpdateAllOfTheBranches(); // updates the branches list
        writeSha1ToCommitsFile(commitSHA1); // append the new sha1 to the commits file

        // add the new commit to the commits tree of the repository
        addCommitNodeToGraph(oldSha1, secondParentSha1,commitSHA1);
        return commitSHA1;
    }

    private void setHeadBranchFileWithCommit(String commitSHA1) throws IOException{

        File file = new File(branchesPath + File.separator + headBranch.getName());
        FilesOperations.writeTextToFile(commitSHA1, file.getPath());
    }

    private void setAllBranchesFileWithNewCommit(String firstCommitSha1) throws IOException
    {
        // this method write the sha1 of the first commit to all of the branches files in branches dir (except HEAD file)
        File branchesDir = new File(branchesPath);
        File[] branchesFiles = branchesDir.listFiles();
        for (File f : branchesFiles)
        {
            if ( !f.getName().equals("HEAD") && !f.getName().equals("commits"))
            {
                FilesOperations.writeTextToFile(firstCommitSha1, f.getPath());
            }
        }
    }

    public String writeCommitToFile(String workingCopySha1,
                                         String firstParentSha1,
                                         String secondParentSha1,
                                         String commitMessage,
                                         String date,
                                         String userName) throws Exception
    {
        String commitInfo = "";
        commitInfo += workingCopySha1 + System.lineSeparator();
        commitInfo += firstParentSha1 + System.lineSeparator();
        commitInfo += secondParentSha1 + System.lineSeparator();
        commitInfo += commitMessage + System.lineSeparator();
        commitInfo += date + System.lineSeparator();
        commitInfo += userName + System.lineSeparator();

        // need to calculate the sha1 of the new commit string
        //String sha1OfTheCommit = SHA1.createSha1(commitInfo);
        String sha1OfTheCommit = DigestUtils.sha1Hex(commitInfo);
        File newCommitFile = new File(objectsPath+ File.separator +sha1OfTheCommit + ".zip");

        try
        {
            if (!newCommitFile.exists())
            {
                //Creating zip file from content instead of from file
                Compressor.createZipFileFromContent(commitInfo, newCommitFile.getPath(), sha1OfTheCommit);
            }
        }
        catch (IOException e)
        {
            throw new IOException("Error: Could not create a temp file for a new commit");
        }
        catch (Exception e)
        {
            throw new Exception("Error: Could not calculate the new commit sha1");
        }

        return sha1OfTheCommit;
    }

    // recursive function only for the fist commit
    public String calculateSha1OfDirectoryFirstCommit(File dir, String userName, String dateOfNow) throws Exception
    {
        File[] filesInThisDir = dir.listFiles();
        String detailsFileContentForThisDir = "";

        // scan all the files and folders under dir
        for ( File f : filesInThisDir )
        {
            // for both folder\blob
            String NameOfObject = f.getName();
            String sha1OfObject = "";
            String type = "";
            String userLastChange = userName;
            String lastChangeTime = dateOfNow;

            if (f.isDirectory())
            {
                //If f is a directory AND f isn't the ".magit" directory AND f isn't an empty directory
                //if the file/directory is empty then we don't want to include it in the commit calculations
                if(! f.getName().equals(".magit") && !FilesOperations.isDirOrFileEmpty(f)){
                    // set all of this directory details
                    // recursively calculate the sha1 of the folder
                    sha1OfObject = calculateSha1OfDirectoryFirstCommit(f, userName, dateOfNow);
                    type = "folder";
                }
            }
            else // f is a file
            {
                // create sha1 from a text file, simple
                sha1OfObject = SHA1.createSha1(f);
                type = "file";
                //we want to create a zip file with the file's content so that we can use it to spread a commit
                File blobFile = new File(objectsPath + File.separator + sha1OfObject);
                String content  = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                //Creating zip file from content instead of from file
                Compressor.createZipFileFromContent(content, blobFile.getPath() + ".zip", sha1OfObject);
            }

            if(! f.getName().equals(".magit") && (f.isFile() || !FilesOperations.isDirOrFileEmpty(f))) {
                // write to the file that describes Dir , all of the folders details
                detailsFileContentForThisDir += NameOfObject + System.lineSeparator();
                detailsFileContentForThisDir += sha1OfObject + System.lineSeparator();
                detailsFileContentForThisDir += type + System.lineSeparator();
                detailsFileContentForThisDir += lastChangeTime + System.lineSeparator();
                detailsFileContentForThisDir += userLastChange + System.lineSeparator();
                detailsFileContentForThisDir += "" + delimiterOfMagitObject + System.lineSeparator();
            }
        } //for loop end
        // calculate the sha1 and rename the file to it
        //String sha1OfTheFileThatDescribesTheDir = SHA1.createSha1(detailsFileContentForThisDir);
        String sha1OfTheFileThatDescribesTheDir = DigestUtils.sha1Hex(detailsFileContentForThisDir);
        File newFolderDetailsFile = new File(objectsPath+ File.separator +sha1OfTheFileThatDescribesTheDir);
        if (!newFolderDetailsFile.exists())
        {
            try
            {
                //Creating zip file from content instead of from file
                Compressor.createZipFileFromContent(detailsFileContentForThisDir, newFolderDetailsFile.getPath() + ".zip", sha1OfTheFileThatDescribesTheDir);
            }
            catch (IOException e)
            {
                throw new IOException("Error: Could not create a file for the folder " + dir.getPath());
            }
        }

        return sha1OfTheFileThatDescribesTheDir;
    }

    // recursive function , for a commit that is not the first one
    public String calculateSha1OfDirectoryNotFirst(File directoryToCalcSha1, String userName, String dateOfNow) throws Exception
    {
        File[] filesInThisDir = directoryToCalcSha1.listFiles();
        String detailsFileContentForThisDir = "";

        // scan all the files and folders under dir
        for ( File f : filesInThisDir )
        {
            // for both folder\blob
            String NameOfObject = f.getName();
            String sha1OfObject = "";
            String type = "";
            String userLastChange ;
            String lastChangeTime ;

            if (f.isDirectory())
            {
                //If f is a directory AND f isn't the ".magit" directory AND f isn't an empty directory
                //if the file/directory is empty then we don't want to include it in the commit calculations
                if(! f.getName().equals(".magit") && !FilesOperations.isDirOrFileEmpty(f)){
                    // set all of this directory details
                    // recursively calculate the sha1 of the folder
                    type = "folder";
                    sha1OfObject = calculateSha1OfDirectoryNotFirst(f, userName, dateOfNow);
                    //type = "folder";
                }
            }
            else // f is a file
            {
                // create sha1 from a text file, simple
                sha1OfObject = SHA1.createSha1(f);
                type = "file";
                // this file has changed
                try{
                    if (checkIfFileOrFolderChanged(f.getPath(),""))
                    {
                        // create a new zipped file for it
                        //we want to create a zip file with the file's content so that we can use it to spread a commit
                        File newZipFile = new File(objectsPath + File.separator + sha1OfObject  + ".zip");
                        // it is possible that only the file name has changed since current commit, and not the content
                        // therefore, there is already a zip file that describes this same content
                        if (! newZipFile.exists())
                        {
                            creataNewZipFileForBlob(sha1OfObject, f.getPath());
                        }
                    }
                }
                catch (FileNotInTree e){
                    //This is a new file that was added relatively to the current spreaded commit
                    File newZipFile = new File(objectsPath + File.separator + sha1OfObject + ".zip");
                    // it is possible that only the file name has changed since current commit, and not the content
                    // therefore, there is already a zip file that describes this same content
                    if (! newZipFile.exists())
                    {
                        creataNewZipFileForBlob(sha1OfObject, f.getPath());
                    }
                }
            }
                // Add this object to the commit file
                if( !f.getName().equals(".magit") && ( f.isFile() || !FilesOperations.isDirOrFileEmpty(f)))
                {
                    // write to the file that describes Dir , all of the folders details
                    detailsFileContentForThisDir += NameOfObject + System.lineSeparator();
                    detailsFileContentForThisDir += sha1OfObject + System.lineSeparator();
                    detailsFileContentForThisDir += type + System.lineSeparator();
                    // depend on if there is a change in f
                    GitObject oldObject = findFileOrFolderInfoInTree(f.getPath()); // get the old object of this
                    if (oldObject != null && oldObject.getmSHA1().equals(sha1OfObject)){
                        // this object was in the WC and didn't change
                        userLastChange = oldObject.getmLastModifiedBy();
                        lastChangeTime = oldObject.getmLastModifiedDate();
                    }
                    else
                    {
                        // wasn't in last commit (new) or was in the last commit and changed
                        userLastChange = userName;
                        lastChangeTime = dateOfNow;
                    }

                    detailsFileContentForThisDir += lastChangeTime + System.lineSeparator();
                    detailsFileContentForThisDir += userLastChange + System.lineSeparator();
                    detailsFileContentForThisDir += "" + delimiterOfMagitObject + System.lineSeparator();
                }
        } //for loop end

        // calculate the sha1 and rename the file to it
        //String sha1OfTheFileThatDescribesTheDir = SHA1.createSha1(detailsFileContentForThisDir);
        String sha1OfTheFileThatDescribesTheDir = DigestUtils.sha1Hex(detailsFileContentForThisDir);
        try{
            if (checkIfFileOrFolderChanged(directoryToCalcSha1.getPath(), detailsFileContentForThisDir))
            {
                createNewZipFileForFolder(sha1OfTheFileThatDescribesTheDir, directoryToCalcSha1.getPath(), detailsFileContentForThisDir);
            }
        }
        catch (FileNotInTree e){ //This is a new folder that was added relatively to the current spreaded commit
            createNewZipFileForFolder(sha1OfTheFileThatDescribesTheDir, directoryToCalcSha1.getPath(), detailsFileContentForThisDir);
        }

        return sha1OfTheFileThatDescribesTheDir;
    }

    private void createNewZipFileForFolder(String sha1OfTheFileThatDescribesTheDir,
                                           String directoryToCalcSha1Path,
                                           String detailsFileContentForThisDir) throws IOException{
        // this means this dir has changed during the commit
        File newFolderDetailsFile = new File(objectsPath+ File.separator +sha1OfTheFileThatDescribesTheDir + ".zip");
        if (!newFolderDetailsFile.exists())
        {
            try
            {
                //Creating zip file from content instead of from file
                Compressor.createZipFileFromContent(detailsFileContentForThisDir, newFolderDetailsFile.getPath(), sha1OfTheFileThatDescribesTheDir);
            }
            catch (IOException e)
            {
                throw new IOException("Error: Could not create a file for the folder " + directoryToCalcSha1Path);
            }
        }
    }
    private void creataNewZipFileForBlob(String sha1OfObject, String filePath) throws IOException{

        File blobFile = new File(objectsPath + File.separator + sha1OfObject);
        String content = FileUtils.readFileToString(new File(filePath),  StandardCharsets.UTF_8);
        //Creating zip file from content instead of from file
        Compressor.createZipFileFromContent(content, blobFile.getPath() + ".zip", sha1OfObject);
    }


    // get xml that has the path of the repository we want to create
    // call this c'tor only if the path in the xml file isn't a magit reposotory already
    public Repository(File xmlFile) {
        // wait for to next class
    }

    public void initializeEmptyRepository (String pathOfRepository,String repoName) throws IOException {
        boolean success;
        headBranch = new Branch("master");
        //  branches list with only "master"
        allOfTheBranches = new ArrayList<>();
        allOfTheBranches.add(headBranch);
        repositoryLocation = pathOfRepository;
        // take the last directory in the repository path, call it "name" however this is not the name!!!
        // we made a mistake so we fixed it like that
        String lastDirOfRepo = pathOfRepository.substring(pathOfRepository.lastIndexOf(File.separator)+1);
        repositoryName = lastDirOfRepo;
        repositoryLogicalNameForUser = repoName;
        magitPath = repositoryLocation + File.separator + ".magit";
        repoNamePath = magitPath + File.separator + "name";
        objectsPath = magitPath + File.separator +"" + "objects";
        branchesPath = magitPath + File.separator + "branches";
        headFilePath = branchesPath + File.separator + "HEAD";
        String commitsFilePath = branchesPath + File.separator + "commits";

        File magitFolder = new File(magitPath);
        if (!magitFolder.exists()) {
            //creates all the way to .magit, including the repository itself, if it doesn't exist
            success = magitFolder.mkdirs();
            Files.setAttribute(Paths.get(magitFolder.getPath()), "dos:hidden", true);

            if (!success) {
                throw new IOException("Could not create the folder " + pathOfRepository + "\nCheck your permissions.");
            } else {

                String masterBranchPath = branchesPath + File.separator + "master";
                File branchesFolder = new File(branchesPath);
                File objectsFolder = new File(objectsPath);
                File masterBranchFile = new File(masterBranchPath);
                File headFile = new File(headFilePath);
                File commitsFile = new File(commitsFilePath);
                File nameFile = new File(repoNamePath);

                if (!objectsFolder.exists())
                    success = objectsFolder.mkdirs();
                if (!branchesFolder.exists())
                    success = success && branchesFolder.mkdirs();
                // create the master branch file
                if (!masterBranchFile.exists())
                    success = success && masterBranchFile.createNewFile();
                if (!headFile.exists())
                    success = success && headFile.createNewFile();
                if(!commitsFile.exists())
                {
                    // create a file to save all the commits sha1 inside.
                    success = success && commitsFile.createNewFile();
                    //for now, the repository is empty so leave the file empty as well.
                }
                // write the branch name to the head file under branched directory
                success = success && FilesOperations.writeTextToFile(headBranch.getName(), headFilePath);
                // create a new file to save repository name, under .magit dir
                if (!nameFile.exists())
                {
                    success = success && nameFile.createNewFile();
                    // write the name to it
                    success = success && FilesOperations.writeTextToFile(repoName , repoNamePath);
                }
                if (!success) {
                    throw new IOException(pathOfRepository + " was created, but could not create its subdirectories.\nCheck your permissions.");
                }

                //Initializing the commit tree
                initializeMagitCommitTree();
            }
        }
    }

    // takes file from the file system and create the magit objects
    public void loadRepositoryFromPath (String pathOfRepository) throws IOException, Exception {

        // read the magit files in this path and load them to the class repository:
        // need to initialize : repository name, repository location, allOfTheBranches, headBranch
        this.repositoryLocation = pathOfRepository;
        setAllRepositoryPath();
        int index = pathOfRepository.lastIndexOf(File.separator);
        repositoryName = pathOfRepository.substring(index + 1); // get the name from the path
        setRepositoryNameFromFile();

        //An exception might be thrown from one of these methods- in this case it will be thrown to the
        //calling method.

        //initialize head branch object and objects tree of the head branch commit
        setHeadBranch();

        if (remoteRepoPath == null || remoteRepoName == null)
        {
            tryToSetRemote(); // maybe there is no remote at all
        }
        //initializing all branches list
        resetAndUpdateAllOfTheBranches();
        //Create commit tree
        initializeMagitCommitTree();
    }

    private void tryToSetRemote() throws IOException
    {
        // try to find a file that describes the remote repo, if exist, set the data member
        File remoteFile = new File(magitPath + File.separator + "remote");
        if (remoteFile.exists())
        {
            String remotePath = FileUtils.readFileToString(remoteFile, StandardCharsets.UTF_8);
            remoteRepoPath = remotePath;
        }

        File remoteNameFile = new File (magitPath + File.separator + "remote-name");
        if (remoteNameFile.exists())
        {
            String remoteName = FileUtils.readFileToString(remoteNameFile, StandardCharsets.UTF_8);
            remoteRepoName = remoteName;
        }
    }

    private void setRepositoryNameFromFile() throws IOException
    {
        File repoNameFile = new File(repoNamePath);
        if(repoNameFile.exists()){
            repositoryLogicalNameForUser = FilesOperations.readStringFromFileUntilEnter(repoNamePath);
        }
    }
    private void  setAllRepositoryPath()
    {
        // update all of the paths we use
        magitPath = repositoryLocation + File.separator + ".magit";
        repoNamePath = magitPath + File.separator + "name";
        objectsPath = magitPath + File.separator +"" + "objects";
        branchesPath = magitPath + File.separator + "branches";
        headFilePath = branchesPath + File.separator + "HEAD";
    }
    // set head branch name and commit, and also the git objects tree of this commit
    private void setHeadBranch () throws IOException {
        setHeadBranchName();
        setHeadBranchCommit();
        initializeGitObjectTree(headBranch.getCommit() == null ? null : headBranch.getCommit().getSha1());
    }


    public Tree initializeGitObjectTreeForCommit(String commitToSpreadSHA1) throws IOException{

        Tree gitObjectTreeForCommit = null;
        if(commitToSpreadSHA1 != null && !commitToSpreadSHA1.isEmpty()){

            //Getting the main repository SHA1 from commit to spread
            File commitToSpreadFile = new File(objectsPath + File.separator + commitToSpreadSHA1 + ".zip");
            String[] commitDetails = FilesOperations.readAllContentFromZipFile(commitToSpreadFile.getPath())
                    .split("\\r?\\n");

            //Creating the root node that represents the main repository folder
            GitObject gitObject = new Folder(repositoryName,
                    repositoryName, //repository name is the path of the repository
                    commitDetails[Commit.mainRepoSHA1], //file name is the SHA1 in this case
                    FolderType.FOLDER,
                    commitDetails[Commit.lastModifiedDate],
                    commitDetails[Commit.username]);
            TreeNode treeRoot = new TreeNode(gitObject, new ArrayList<>(), null);
            //Calling the recursive method with the root info (main repository info)
            createGitObjectTree(treeRoot);
            gitObjectTreeForCommit = new Tree(treeRoot);
        }
        else{
            gitObjectTreeForCommit = null;
        }

        return gitObjectTreeForCommit;
    }

    //This method gets the main repository path, and then calls the recursive method
    // that gets the path of the main repository and creates a GitObject tree that represents the repository structure.
    //The gitObjectTree is always according to a specific commit
    public void initializeGitObjectTree(String commitToSpreadSHA1) throws IOException {

        if(commitToSpreadSHA1 != null && !commitToSpreadSHA1.isEmpty()){

            //Getting the main repository SHA1 from commit to spread
            File commitToSpreadFile = new File(objectsPath + File.separator + commitToSpreadSHA1 + ".zip");
            String[] commitDetails = FilesOperations.readAllContentFromZipFile(commitToSpreadFile.getPath())
                    .split("\\r?\\n");

            //Creating the root node that represents the main repository folder
            GitObject gitObject = new Folder(repositoryName,
                    repositoryName, //repository name is the path of the repository
                    commitDetails[Commit.mainRepoSHA1], //file name is the SHA1 in this case
                    FolderType.FOLDER,
                    commitDetails[Commit.lastModifiedDate],
                    commitDetails[Commit.username]);
            TreeNode treeRoot = new TreeNode(gitObject, new ArrayList<>(), null);
            //Calling the recursive method with the root info (main repository info)
            createGitObjectTree(treeRoot);
            gitObjectTree = new Tree(treeRoot);
        }
        else{
            gitObjectTree = null;
        }
    }

    private TreeNode createGitObjectTree(TreeNode root) throws IOException{
        //Getting the information about the content of the current main folder/file that is processed
        File mainFolder = new File(objectsPath + File.separator + root.getmNode().getmSHA1() + ".zip");
        String[] mainFolderContent = FilesOperations.readAllContentFromZipFile(mainFolder.getPath())
                .split("\\r\n;\\r\n"); //Splitting each file/folder into string array

        //In case of empty repository
        if(!mainFolderContent[0].equals("")){
            //Iterating over all folders/files in main folder
            for(String string : mainFolderContent){

                //Splitting current element info to string array
                String[] currentFileToCheck = string.split("\\r?\\n"); //Extracting each directory/ file info
                String fileToCheckPath = repositoryLocation + File.separator + currentFileToCheck[GitObject.SHA1] + ".zip";
                File fileToCheck = new File(fileToCheckPath);

                //If the current element is a folder then create a node for it with type FOLDER.
                //and call the recursive method with it
                if(currentFileToCheck[GitObject.type].equals("folder")){
                    GitObject folderGitObject = new Folder(
                            currentFileToCheck[GitObject.fileName],
                            root.getmNode().getmPath() + File.separator + currentFileToCheck[GitObject.fileName],
                            currentFileToCheck[GitObject.SHA1],
                            FolderType.FOLDER,
                            currentFileToCheck[GitObject.lastModifiedDate],
                            currentFileToCheck[GitObject.username]);
                    TreeNode folderNode = new TreeNode(folderGitObject, new ArrayList<>(), root);
                    TreeNode child = createGitObjectTree(folderNode); //Calling recursive method with the folder node
                    child.setmParent(root);
                    root.addChild(child);
                }
                else{//current element is a file- create file node
                    GitObject fileGitObject = new Blob(
                            currentFileToCheck[GitObject.fileName],
                            root.getmNode().getmPath() + File.separator + currentFileToCheck[GitObject.fileName],
                            currentFileToCheck[GitObject.SHA1],
                            FolderType.FILE,
                            currentFileToCheck[GitObject.lastModifiedDate],
                            currentFileToCheck[GitObject.username]);
                    TreeNode fileNode = new TreeNode(fileGitObject, new ArrayList<>(), root);
                    fileNode.setmParent(root);
                    root.addChild(fileNode);
                }
            }
        }

        return root;
    }

    //This method accepts a valid path of file/folder (testing whether the path exists or not should
    //be done before calling this method)
    //it calls the recursive method that finds the GitObject in the GitObjectTree
    public GitObject findFileOrFolderInfoInTreeForCommit(String pathInMainRepo, Tree gitObjectTree){

        GitObject res = null;
        //Getting the path split by file separator- beginning with the repository name as the main folder
        //rather than C:/... because the root GitObject in the tree represents the main repository.
        int indexOfRepositoryName = pathInMainRepo.indexOf(repositoryName);
        //Extracting the path beginning with the repository name and not full path
        String pathToCheck = pathInMainRepo.substring(indexOfRepositoryName);
        String[] pathHierarchy = pathToCheck.split(Pattern.quote(File.separator));

        //If the path is in of the current repository that's loaded
        if(gitObjectTree.getRoot().getmNode().getmName().equals(pathHierarchy[0])){
            String[] innerPathHierarchy = Arrays.copyOfRange(pathHierarchy, 1, pathHierarchy.length);
            res = findGitObjectInTreeByPath(innerPathHierarchy, gitObjectTree.getRoot());
        }

        return res;
    }

    //This method accepts a valid path of file/folder (testing whether the path exists or not should
    //be done before calling this method)
    //it calls the recursive method that finds the GitObject in the GitObjectTree
    public GitObject findFileOrFolderInfoInTree(String pathInMainRepo){

        GitObject res = null;
        //Getting the path split by file separator- beginning with the repository name as the main folder
        //rather than C:/... because the root GitObject in the tree represents the main repository.
        int indexOfRepositoryName = pathInMainRepo.indexOf(repositoryName);
        //Extracting the path beginning with the repository name and not full path
        String pathToCheck = pathInMainRepo.substring(indexOfRepositoryName);
        String[] pathHierarchy = pathToCheck.split(Pattern.quote(File.separator));

        //If the path is in of the current repository that's loaded
        if(gitObjectTree.getRoot().getmNode().getmName().equals(pathHierarchy[0])){
            String[] innerPathHierarchy = Arrays.copyOfRange(pathHierarchy, 1, pathHierarchy.length);
            res = findGitObjectInTreeByPath(innerPathHierarchy, gitObjectTree.getRoot());
        }

        return res;
    }

    private GitObject findGitObjectInTreeByPath(String[] pathHierarchy, TreeNode root) {

        GitObject res = null;

        if(root == null){
            return null;
        }
        //If we're looking for a file gitObject
        else if(root.getmChildren() == null || root.getmChildren().size() == 0){

            return pathHierarchy.length == 0 ? root.getmNode() : null;
        }
        //If we're looking for a folder gitObject
        else if(pathHierarchy.length == 0){
            return root.getmNode();
        }
        else
        {
            for(TreeNode child : root.getmChildren()){
                if (child.getmNode().getmName().equals(pathHierarchy[0])){
                    //Getting path of file without current root folder
                    String[] innerPathHierarchy = Arrays.copyOfRange(pathHierarchy, 1, pathHierarchy.length);
                    res = findGitObjectInTreeByPath(innerPathHierarchy, child);
                    if(res != null) break;
                }
            }
        }

        return res;
    }

    public boolean checkIfFileOrFolderChangedForCommit(String fullFilePath, String detailsFileContentForThisDir, Tree gitObjectTree, String pathNameToSpread) throws Exception {

        GitObject res = null;
        String prevFileSHA1 = "";
        String currFileSHA1 = "";
        File currFileToCheck = null;

        if(gitObjectTree != null){

            //The file we want to check maybe in temp file, and its full path won't fit to the full paths in the gitObject tree
            String relativeToTreeFilePath = getTreeRelativePath(fullFilePath, pathNameToSpread);
            if (!((res = findFileOrFolderInfoInTreeForCommit(relativeToTreeFilePath, gitObjectTree)) == null)) {
                prevFileSHA1 = res.getmSHA1();
                if(res.getmFolderType()== FolderType.FOLDER){
                    if(!detailsFileContentForThisDir.isEmpty()){
                        currFileSHA1 = DigestUtils.sha1Hex(detailsFileContentForThisDir);
                    }
                }
                else{ //if res is a file gitObject
                    currFileToCheck = new File(fullFilePath);
                    currFileSHA1 = SHA1.createSha1(currFileToCheck);
                }

                res.setWasChecked(true);

            } else {
                throw new FileNotInTree(fullFilePath);
            }
        }
        else{ //if the git object tree isn't even initialized
            return(true);
        }

        return (!prevFileSHA1.equals(currFileSHA1));
    }

    public String getTreeRelativePath(String filePath, String pathNameToSpread){
        String res = "";
        String[] arr = filePath.split("\\\\.magit\\\\"+ pathNameToSpread);
        res = arr[0].concat(arr[1]);
        return res;
    }

    public boolean checkIfFileOrFolderChanged(String fullFilePath, String detailsFileContentForThisDir) throws Exception {

        GitObject res = null;
        String prevFileSHA1 = "";
        String currFileSHA1 = "";
        File currFileToCheck = null;

        if(gitObjectTree != null){

            if (!((res = findFileOrFolderInfoInTree(fullFilePath)) == null)) {
                prevFileSHA1 = res.getmSHA1();
                if(res.getmFolderType()== FolderType.FOLDER){
                    if(!detailsFileContentForThisDir.isEmpty()){
                        currFileSHA1 = DigestUtils.sha1Hex(detailsFileContentForThisDir);
                    }
                }
                else{ //if res is a file gitObject
                    currFileToCheck = new File(fullFilePath);
                    currFileSHA1 = SHA1.createSha1(currFileToCheck);
                }

                res.setWasChecked(true);

            } else {
                throw new FileNotInTree(fullFilePath);
            }
        }
        else{ //if the git object tree isn't even initialized
            return(true);
        }

        return (!prevFileSHA1.equals(currFileSHA1));

    }

    //Goes to HEAD file in "branches" directory- reads the branch name from the HEAD file and set headBranchName
    private void setHeadBranchName() throws IOException{

        try {
            headBranch.setName(FilesOperations.readStringFromFileUntilEnter(headFilePath));
        } catch (IOException e) {
            throw new IOException("Couldn't read from file " + headFilePath + "\nCheck your permissions");
        }
    }

    // set the head branch commit object
    //Goes to head branch file (whose name is the head branch name), reads the commit SHA1 pointed by the head branch
    //and then gets all commit details from the commit file in "objects" directory (with getCommitBySHA1 method)
    private void setHeadBranchCommit () throws IOException {

        String branchFilePath = branchesPath + File.separator + headBranch.getName();
        String commitSHA1 = "";
        try {
            commitSHA1 = FilesOperations.readStringFromFileUntilEnter(branchFilePath) == null ? ""
                    : FilesOperations.readStringFromFileUntilEnter(branchFilePath);

        } catch (IOException e) {
            throw new IOException("\nCouldn't read from file " + branchFilePath + "\nCheck your permissions");
        }

        //There might be an exception from getCommitBySHA1, in this case it will be thrown to the
        //calling method.
        Commit commit = getCommitBySHA1(commitSHA1);
        headBranch.setCommit(commit);
    }

    // Clears allOfTheBranches list and adds to it new branches (including head branch) by going over all branch files
    // and taking only the SHA1 of the commit (not all commit details)
    public synchronized void resetAndUpdateAllOfTheBranches () throws IOException, Exception {

        //Cleaning current allOfTheBranches list and adding head branch to it
        allOfTheBranches.clear();
        allOfTheBranches.add(headBranch);

        //Iterating over all other branches (without HEAD file and without the branch file that is the head branch)
        String commitSHA1 = "";
        File dir = new File(branchesPath);
        File[] directoryListing = dir.listFiles(); //Array of all files in the "branches" directory
        if (directoryListing != null) {
            for (File branchFile : directoryListing) {

                //We don't want to process HEAD file and the branch file that is the head branch
                // iterating local branch (simple local branch)
                if (!branchFile.isDirectory() && !(branchFile.getName().equals(headBranch.getName()) || branchFile.getName().equals("HEAD") || branchFile.getName().equals("commits"))) {
                    String branchName = branchFile.getName();
                    try {
                        commitSHA1 = FilesOperations.readStringFromFileUntilEnter(branchFile.getPath());


                        String branchInfoFilePath =  objectsPath + File.separator + commitSHA1 + ".zip";
                        String zipFileContent = FilesOperations.readAllContentFromZipFile(branchInfoFilePath);
                        Commit branchCommit;
                        if (zipFileContent != null){
                            //Getting all commit details from commit file
                            String lines[] = zipFileContent.split("\\r?\\n");
                            branchCommit = new Commit(commitSHA1, lines[0], lines[1], lines[2], lines[3], lines[4], lines[5]);
                        }
                        else
                        {
                            branchCommit =  new Commit();
                        }
                        allOfTheBranches.add(new Branch(branchName,branchCommit ));

                    } catch (IOException e) {
                        throw new IOException("Couldn't read from file " + branchFile.getPath() + "\nCheck your permissions\n");
                    }
                }
            }
            // if this repo is a cloning of another repo, then add all the remote branches to the list
            if (remoteRepoPath != null)
                addRemoteBranchesToBranchesList();
        } else {
            // should never get here, just a sanity check
            throw new Exception("Your 'branches' folder seems to be empty");
        }
    }

    // is called only when the remote branches directory exists
    private void addRemoteBranchesToBranchesList() throws IOException
    {
        String remoteBranchesFilesPath = branchesPath + File.separator + remoteRepoName;
        File[] remoteBranchesFiles = new File(remoteBranchesFilesPath).listFiles();
        //The remote branches directory may not exist in this point (we are calling this method ALSO from initializeEmptyRepo)
        if(new File(remoteBranchesFilesPath).exists()){
            for (File remoteBranchFile : remoteBranchesFiles)
            {
                // read the sha1 from the remote branch file
                String sha1OfRemoteBranch = FileUtils.readFileToString(remoteBranchFile, StandardCharsets.UTF_8);
                //Getting commit details to put in remote branch
                String branchInfoFilePath =  objectsPath + File.separator + sha1OfRemoteBranch + ".zip";
                String zipFileContent = FilesOperations.readAllContentFromZipFile(branchInfoFilePath);
                Commit remoteBranchCommit;
                if (zipFileContent != null){
                    //Getting all commit details from commit file
                    String lines[] = zipFileContent.split("\\r?\\n");
                    remoteBranchCommit = new Commit(sha1OfRemoteBranch, lines[0], lines[1], lines[2], lines[3], lines[4], lines[5]);
                }
                else
                {
                    remoteBranchCommit =  new Commit();
                }
                Branch newRB = new Branch(remoteRepoName + File.separator + remoteBranchFile.getName(), remoteBranchCommit);
                newRB.setIsRemote(true);
                allOfTheBranches.add(newRB);
            }
        }
    }

    public Commit getCommitBySHA1(String commitSHA1) throws IOException {

        Commit commit = null;
        String commitFilePath = objectsPath + File.separator + commitSHA1;
        //The commit SHA1 could be empty in case we're trying to load
        // an empty repository in which no commits were ever made
        if (!(commitSHA1.isEmpty() || commitSHA1.length() != SHA1.sha1Size)) {
            try {
                //Reading content of zip file without unzipping it
                String zipFileContent = FilesOperations.readAllContentFromZipFile(commitFilePath + ".zip");

                if (zipFileContent != null){
                    //Getting all commit details from commit file
                    String lines[] = zipFileContent.split("\\r?\\n");
                    commit = new Commit(commitSHA1, lines[0], lines[1], lines[2], lines[3], lines[4], lines[5]);
                }

            } catch (IOException e) {
                throw new IOException("Couldn't read from file " + commitFilePath + "\nCheck your permissions");
            }
        }

        return commit;
    }

    public boolean isThereAnyCommitInTheRepository (String repositoryPath) throws IlegalHeadFileException {
        boolean res = false;

        try {
            String headFileContent = FilesOperations.readStringFromFileUntilEnter(headFilePath);
            if (headFileContent.isEmpty()) {
                res = false;
                throw new IlegalHeadFileException(headFilePath);
            } else {
                // the HEAD file has the curent branch name
                String currentBranchFilePath = branchesPath + File.separator + headFileContent;
                // read the sha1 of the current branch
                String headSha1 = FilesOperations.readStringFromFileUntilEnter(currentBranchFilePath);
                if (headSha1.length() == SHA1.sha1Size && !headSha1.contains(" "))
                    res = true;
                else {
                    // the current branch doesn't have a commit
                    res = false;
                }
            }
        } catch (IOException e) {
            res = false;
        } finally {
            return res;
        }
    }

    public String getRepositoryLocation () {
        return repositoryLocation;
    }

    public ArrayList<Branch> getAllOfTheBranches () {
        return allOfTheBranches;
    }

    public Branch getHeadBranch () {
        return headBranch;
    }

    public ArrayList<String> getHistoryOfActiveBranch() throws IOException
    {
        ArrayList<String> history = new ArrayList<>();
        Commit currentCommit = headBranch.getCommit();
        while (currentCommit != null) // there is any commit on the active branch
        {
            String commitDetail = new String("Commit " + currentCommit.getSha1());
            // point to the current head position
            if (currentCommit.equals(headBranch.getCommit()))
            {
                commitDetail += "(HEAD ---> " + headBranch.getName()+ ")";
            }
            commitDetail += "\nAuthor: " + currentCommit.getmCreatedBy();
            commitDetail += "\nDate: " + currentCommit.getmCreatedDate();
            commitDetail += "\n\n    " + currentCommit.getmMessage() + "\n\n";
            history.add(commitDetail);
            // go to the parent commit, in Ex 1 we have only one pareny for each commit
            String sha1OfParent = currentCommit.getFirstParentSha1();
            if (!sha1OfParent.equals("null"))
            {
                currentCommit = getCommitBySHA1(sha1OfParent);
            }
            else
            {
                // the parent is null
                currentCommit = null;
            }
        }
        return history;
    }

    public void putCommitContentOnTempFolder(String commitSha1, File tempFolder) throws IOException{

        // the file that describes the commit
        String commitFilePath = objectsPath + File.separator + commitSha1 + ".zip";
        String workingCopySha1;
        try
        {
            // read first line, where the root folder sha1 is
            ArrayList<String> stringsOfCommitFile = FilesOperations.readAllContentFromZipFileByStrings(commitFilePath);
            workingCopySha1 = stringsOfCommitFile.get(0);
            // first must delete all the working copy files and directories, except the folder .magit
            // read from the given working copy zip file all the root objects
            File workingCopyDescriptionFile = new File(objectsPath + File.separator + workingCopySha1 + ".zip"); // the path to the file that describes the WC
            ArrayList<GitObject> filesOfRoot = getAllObjectsOfDirectory(workingCopyDescriptionFile);

            // extract each git object content to the desired path
            for (GitObject obj : filesOfRoot)
            {
                putContentOfGitObjectToDir(obj, tempFolder.getPath() );
            }
        }
        // might be thrown by readStringFromFileUntilEnter or getAllObjectsOfDirectory
        catch (IOException e)
        {
            throw e;
        }
    }

    public void putCommitContentOnWorkingCopy(String commitSha1) throws IOException
    {
        // the file that describes the commit
        String commitFilePath = objectsPath + File.separator + commitSha1 + ".zip";
        String workingCopySha1;
        try
        {
            // read first line, where the root folder sha1 is
            ArrayList<String> stringsOfCommitFile = FilesOperations.readAllContentFromZipFileByStrings(commitFilePath);
            workingCopySha1 = stringsOfCommitFile.get(0);
            // first must delete all the working copy files and directories, except the folder .magit
            if (! deleteWorkingCopy(repositoryLocation))
                throw new IOException("Could not delete some of the files in the working copy.");
            // read from the given working copy zip file all the root objects
            File workingCopyDescriptionFile = new File(objectsPath + File.separator + workingCopySha1 + ".zip"); // the path to the file that describes the WC
            ArrayList<GitObject> filesOfRoot = getAllObjectsOfDirectory(workingCopyDescriptionFile);

            // extract each git object content to the working tree recursively, starting from the repository path
            for (GitObject obj : filesOfRoot)
            {
                putContentOfGitObjectToDir(obj, repositoryLocation);
            }
        }
        // might be thrown by readStringFromFileUntilEnter or getAllObjectsOfDirectory
        catch (IOException e)
        {
            throw e;
        }
    }

    // the argument directoryDescriptionFile  - must be a path to a zip file with the extension ".zip" that describes a directory
    public static ArrayList<GitObject> getAllObjectsOfDirectory(File directoryDescriptionFile) throws IOException
    {
        ArrayList<GitObject> allFilesOfADirectory = new ArrayList<>();
        String name, sha1, type, date, userChanged;
        FolderType fType;
        //read all file descriptor from the file that describes the commit
        ArrayList<String> linesOfZipFile = FilesOperations.readAllContentFromZipFileByStrings(directoryDescriptionFile.getPath());
        // iterate over all the lines of the commit file, i++ skip the seperating ";" line
        for ( int i =0 ; i < linesOfZipFile.size(); i++)
        {
            // for each field get it, and promote i, skip the ";"
            name = linesOfZipFile.get(i++); // read name
            sha1 = linesOfZipFile.get(i++); // read sha1
            type = linesOfZipFile.get(i++); // read type
            fType = FolderType.toFolderType(type); // convert type from string to folder type
            date = linesOfZipFile.get(i++); // read last changing date
            userChanged = linesOfZipFile.get(i++); // read user changed,
            // add the gitobject to the list
            allFilesOfADirectory.add(new GitObject(name,directoryDescriptionFile.getPath(), sha1, fType, date, userChanged));
        }
        return allFilesOfADirectory;
    }

    public void putContentOfGitObjectToDir(GitObject gitObject, String directoryToPutAllFilesIn) throws IOException
    {
        // new object is a folder or a file, not a zip file! a real text file.
        String newObjectPath = directoryToPutAllFilesIn + File.separator + gitObject.getmName();
        File newFile = new File(newObjectPath);
        if (gitObject.getmFolderType() == FolderType.FILE)
        {
            // create the file and write the relevant content to it
            if (! newFile.exists()) // the wc should be empty, this is just a sanity check
            {
                // read the content of the blob from the zipped file under objects directory
                String blobContent = FilesOperations.readAllContentFromZipFile(objectsPath + File.separator + gitObject.getmSHA1() + ".zip");
                newFile.createNewFile(); // create the new file on the wc
                FilesOperations.writeTextToFile(blobContent, newObjectPath); // write the content to the text file on the WC
            }
            else
            {
                throw new IOException("Error: The file " + newObjectPath + " already exist.");
            }
        }
        else // a directory
        {
            // create the dir in the WC
            if (! newFile.exists())
            {
                newFile.mkdirs();
                // the zip file that describe the folder, named by its sha1
                File thisDir = new File(objectsPath + File.separator + gitObject.getmSHA1() + ".zip");
                ArrayList<GitObject> filesOfThisDir = getAllObjectsOfDirectory(thisDir);

                // extract each git object content of this dir  to the working tree recursively, starting from the dir path
                for (GitObject obj : filesOfThisDir)
                {
                    //recursive call
                    // put the files recursively under the new directory
                    putContentOfGitObjectToDir(obj, newObjectPath);
                }
            }
            else
            {
                throw new IOException("Error: The folder " + newObjectPath + " already exist.");
            }
        }
    }
    public boolean deleteWorkingCopy(String workingCopyLocation)
    {
        File repositoryFolder = new File(workingCopyLocation);

        File[] files = repositoryFolder.listFiles();
        for (File f : files)
        {
            if ( f.isDirectory() && ! f.getName().equals(".magit"))
            {
                FilesOperations.deleteFolder(f);
            }
            else if (! f.isDirectory()) // f is a file
            {
                try {
                    Path filePath = Paths.get(f.getPath());
                    Files.delete(filePath);
                }
                catch (Exception e){
                    return false;
                }
            }
        }
        return true;
    }

    public void checkOutToBranch(String branchNameToCheckout) throws CommitIsNullException, Exception {
        // perform check out to a legal branch, different than the head branch
        // if they both points to the same commit, we don't put the commit content on working copy ( redundant)
        // get the commit of the desired branch
        String commitSha1ToCheckout = FilesOperations.readStringFromFileUntilEnter(branchesPath + File.separator + branchNameToCheckout);
        if (commitSha1ToCheckout == null || commitSha1ToCheckout.isEmpty() )
        {
            // means the user tried to checkout to a branch that has a null commit
            // this should never happen since null commit is only on the first commit
            throw new CommitIsNullException(branchNameToCheckout);
        }
        else
        {
            putCommitContentOnWorkingCopy(commitSha1ToCheckout);
            headBranch.setName(branchNameToCheckout);    // update the object - head branch name
            setHeadBranchCommit();                      //  update the object - head branch commit
            FilesOperations.writeTextToFile(headBranch.getName(), headFilePath); // write the new active branch name to the HEAD file
            resetAndUpdateAllOfTheBranches();
            initializeGitObjectTree(headBranch.getCommit() == null ? null : headBranch.getCommit().getSha1());
        }
    }
    public void deleteBranch(String branchToDelete) throws IOException, Exception
    {
        // this function is called only with a branch that is not the head branch
        if (allOfTheBranches.size() == 1)
        {
            throw new DeleteLastBranch(branchToDelete);
        }
        deleteBranchFile(branchToDelete); // delete the branch file
        resetAndUpdateAllOfTheBranches(); //  update the branches list
        //recreate the graph is more easy then decide what nodes to delete
        initializeMagitCommitTree();
    }

    private void deleteBranchFile(String branchToDelete) throws IOException
    {
        File branchFileToDelete = new File(branchesPath + File.separator + branchToDelete);
        if (branchFileToDelete.exists())
        {
            boolean res = branchFileToDelete.delete();
            if ( res == false )
            {
                // couldn't delete for some reason
                throw new IOException("Could not remove the file " + branchFileToDelete.getPath());
            }
        }
    }

    public void resetHeadBranchSha1(String sha1ToReset) throws Exception
    {
        putCommitContentOnWorkingCopy(sha1ToReset);         // load to working copy

        //this function reset the head branch sha1 to the sha1ToReset, only sha1 (not name) is changed
        Commit newHeadCommit = getCommitBySHA1(sha1ToReset);

        headBranch.setCommit(newHeadCommit); // update sha1 to the head branch commit object
        // update the branch file
        FilesOperations.writeTextToFile(sha1ToReset, branchesPath + File.separator + headBranch.getName());
        resetAndUpdateAllOfTheBranches();  // update branches list
        initializeGitObjectTree(headBranch.getCommit() == null ? null : headBranch.getCommit().getSha1()); // update the tree
        //recreate the graph is more easy then decide what nodes to delete
        initializeMagitCommitTree();
    }

    public void resetBranchThatIsNotHead(String branchName, String newSha1) throws IOException, Exception
    {
        Commit newCommitOfBranch = getCommitBySHA1(newSha1);
        Branch branchToReset = getBranchByName(branchName);
        branchToReset.setCommit(newCommitOfBranch);
        FilesOperations.writeTextToFile(newSha1, branchesPath + File.separator + branchName);
        resetAndUpdateAllOfTheBranches();  // update branches list
        initializeGitObjectTree(headBranch.getCommit() == null ? null : headBranch.getCommit().getSha1()); // update the tree
        initializeMagitCommitTree();
    }

    public String getRepositoryName() {
        return repositoryLogicalNameForUser;
    }

    public String getRepoFolderName()
    {
        return repositoryName;
    }
    public String getBranchesPath() { return  branchesPath;}
    public Tree getGitObjectTree() {
        return gitObjectTree;
    }
    public String getObjectsPath() {
        return objectsPath;
    }

    public String getMagitPath() {
        return magitPath;
    }

    public MagitCommitTree getCommitTree() {
        return commitTree;
    }


    // This function start building the graph from the commits pointed by branches, and all the way up
    // to the most old commit, which has no parents.
    // it calls a recuresive method
    public void initializeMagitCommitTree() throws IOException{

        commitTree = new MagitCommitTree(); // initialize the tree from last time

        // dictionary for only commits that are the top of some branch, they have no kids.
        Dictionary<String, MagitCommitNode> branchesTopsNodes = getAllBranchesTopNodesDictionary();

        // iterate over all the branches top in the dictionary
        Enumeration<String> sha1s = branchesTopsNodes.keys();
        String sha1OfCommit;
        while (sha1s.hasMoreElements())
        {
            sha1OfCommit = sha1s.nextElement(); // current sha1

            if (! commitTree.isCommitSha1InTree(sha1OfCommit))
            {
                // this commit is not in the commits tree, add it now
                // it has all the branches it points to from the getAllBranchesTopNodesDictionary implementation
                MagitCommitNode newNode = branchesTopsNodes.get(sha1OfCommit);
                commitTree.addNodeToDictionary(sha1OfCommit, newNode);

                if( newNode.getCommit().getFirstParentSha1() != null && !newNode.getCommit().getFirstParentSha1().equals("null"))
                {
                    // the node has a first parent
                    MagitCommitNode firstParent = recursiveAddParentNodeToTree(newNode.getCommit().getFirstParentSha1(), newNode);
                    newNode.setFirstParent(firstParent);
                }
                else
                {
                    // if this commit doesn't have a first parent, which is mandatory, then it the the root of the tree
                    commitTree.setRoot(newNode);
                }
                if (newNode.getCommit().getSecondParentSha1() != null && !newNode.getCommit().getSecondParentSha1().equals("null"))
                {
                    // the node has a second parent
                    MagitCommitNode secondParent = recursiveAddParentNodeToTree(newNode.getCommit().getSecondParentSha1(), newNode);
                    newNode.setSecondParent(secondParent);
                }
            }
            else
            {
                MagitCommitNode node = commitTree.getSha1ToNodesDictionary().get(sha1OfCommit);
                {
                    if(node != null )
                    {
                        node.setBranchesThatHasMe(branchesTopsNodes.get(sha1OfCommit).getBranchesThatHasMe());
                    }
                }
            }
            /* else- the commit was added to the commits tree by another branch top commit
             * so there is no need to add it again, but only set its branches list to what we have found already
             * For example: this graph:
             *          (3) - branch master
             *           |
             *           |
             *           v
             *          (2) - branch yuval
             *           |
             *           |
             *           v
             *          (1) - very first commit..
             *
             *  In this example, commit number 3 will recursively add commit 2 to the graph already
             * */
        }
    }

    // This method create non-existing node in the dictionary, and recursively all its parents.
    public MagitCommitNode recursiveAddParentNodeToTree(String parentSha1, MagitCommitNode mySon) throws IOException
    {
        MagitCommitNode currentNode = commitTree.getSha1ToNodesDictionary().get(parentSha1);

        if ( currentNode == null)
        {
            // the parent is not in the tree, add it now
            Commit currentCommit = getCommitBySHA1(parentSha1);
            currentNode = new MagitCommitNode(currentCommit);
            // add the node to the dictionary
            commitTree.getSha1ToNodesDictionary().put(parentSha1, currentNode);

            if(currentCommit.getFirstParentSha1() != null && !currentCommit.getFirstParentSha1().equals("null"))
            {
                // This commit has a first parent, it is not the root, keep recursion
                MagitCommitNode firstParent = recursiveAddParentNodeToTree(currentCommit.getFirstParentSha1(), currentNode);
                currentNode.setFirstParent(firstParent);
            }
            else
            {
                // if this commit doesn't have a first parent, which is mandatory, then it the the root of the tree
                commitTree.setRoot(currentNode);
            }
            if (currentCommit.getSecondParentSha1() != null && !currentCommit.getSecondParentSha1().equals("null"))
            {
                // This commit has a second parent, it is not the root, keep recursion
                MagitCommitNode secondParent = recursiveAddParentNodeToTree(currentCommit.getSecondParentSha1(), currentNode);
                currentNode.setSecondParent(secondParent);
            }
            return currentNode;
        }
        else
        {
            // the parent node is already in the dictionary and therefor it is in the tree
            // return it
            return currentNode;
        }
    }
    // return a dictionary of all the commits nodes of the branches,
    // those commits are the top of each branch
    // we avoid duplicate nodes with same sha1 ( duplicate commits)
    public Dictionary<String, MagitCommitNode> getAllBranchesTopNodesDictionary() throws IOException
    {
        Dictionary<String, MagitCommitNode> res = new Hashtable<>();
        MagitCommitNode currNode;

        for (Branch b: allOfTheBranches)
        {
            // non empty commit
            if (b.getCommit() != null && b.getCommit().getSha1() != null && !b.getCommit().getSha1().equals("null"))
            {
                //  this sha1 exist in the dictionary
                if ((currNode = res.get(b.getCommit().getSha1()) )!= null)
                {
                    // if this node exist in the commit graph add only the name of the branch that points to it
                    currNode.addBranchToList(b.getName());
                }
                else
                {
                    // this node isn't in the res yet, add it
                    Commit commit = getCommitBySHA1(b.getCommit().getSha1());
                    MagitCommitNode newNode = new MagitCommitNode(commit);
                    newNode.addBranchToList(b.getName());
                    res.put(b.getCommit().getSha1(), newNode);
                }
            }
        }
        return res;
    }

    // this function is called when we want to add a commit (not first one)
    // ATTENTION : the commit is NOT a merge commit!
    private void addCommitNodeToGraph(String oldCommitSha1, String secondParentSha1, String newCommitSha1) throws IOException
    {
        Commit newCommit = getCommitBySHA1(newCommitSha1);

        MagitCommitNode newCommitNode = new MagitCommitNode(newCommit);
        MagitCommitNode secondParentOfNewCommit = null;

        MagitCommitNode firstParentOfNewCommit = commitTree.getSha1ToNodesDictionary().get(oldCommitSha1);
        if (secondParentSha1 != null)
        {
            secondParentOfNewCommit = commitTree.getSha1ToNodesDictionary().get(secondParentSha1);
        }
        if (firstParentOfNewCommit != null)
        {
            newCommitNode.setFirstParent(firstParentOfNewCommit);
            if (secondParentOfNewCommit != null)
            {
                newCommitNode.setSecondParent(secondParentOfNewCommit);
            }
            // now the active branch is pointing to the new node and not to its parent
            String branchNameToMoveToChild = headBranch.getName();
            firstParentOfNewCommit.getBranchesThatHasMe().remove(branchNameToMoveToChild);
            newCommitNode.addBranchToList(branchNameToMoveToChild);
            commitTree.addNodeToDictionary(newCommitSha1, newCommitNode);
        }
        // else should never happen cause it is not the first commit, this is a sanity check
    }

    // when adding a new branch, the node should know it is points to it also
    private void addNewBranchToTreeWhenCommitExist(String branchName, String commitSha1)
    {
        MagitCommitNode commitNode = commitTree.getSha1ToNodesDictionary().get(commitSha1);
        commitNode.addBranchToList(branchName);
    }

    public void addNewBranch(Branch newBranch)
    {
        allOfTheBranches.add(newBranch);
        if (headBranch.getCommit()!= null && !headBranch.getCommit().getSha1().equals("null"))
        {
            addNewBranchToTreeWhenCommitExist(newBranch.getName(), headBranch.getCommit().getSha1());
        }
    }

    public MagitCommitNode addBranchToCommitTree(String branchName) throws CommitTreeException
    {
        if(headBranch.getCommit() != null)
        {
            // head points to an existing commit, so add  it to the list of branches top in the tree
            MagitCommitNode headNode = commitTree.getSha1ToNodesDictionary().get(headBranch.getCommit().getSha1());
            if (headNode != null)
            {
                // we found the magit commit node of the head, add the branch to its list
                if (! headNode.getBranchesThatHasMe().contains(branchName))
                {
                    headNode.addBranchToList(branchName);
                    return headNode;
                }
            }
            else
            {
                // for some reason we did not find the magit commit node in the tree, it means something is wrong with the tree
                throw new CommitTreeException("The head branch does not have a node in the tree.\n" + "Try to load repository again.");
            }
        }
        return null;
    }

    public Commit getCommitOfBranch(String branchName)
    {
        for (Branch b: allOfTheBranches)
        {
            if(b.getName().equals(branchName))
            {
                if (b.getCommit()!= null)
                {
                    return b.getCommit();
                }
                else
                {
                    return null;
                }
            }
        }
        return null;
    }

    // We have copied the remote repo, and now we create new files for each remote branch
    public void cretaeRemoteBranchesFilesAndDeleteLocalBranchesThatAreNotHead(String repoPath, String remoteRepoName) throws IOException
    {
        // need to create a folder for the remote, and put in it all the Remote Branches
        String magitPath = repoPath + File.separator + ".magit";
        String branchesPath = magitPath + File.separator + "branches" ;
        String remoteDirPath = branchesPath + File.separator + remoteRepoName;

        remoteDirPath.replace(System.lineSeparator(), "");
        createRBDirectory(remoteDirPath);

        // iterate all over the local branches files we just copied, and create a RB file for each one
        File branchesDir = new File(branchesPath);

        File[] localBranchesFiles = branchesDir.listFiles();
        // get the head branch name
        String headBranchName = FileUtils.readFileToString(new File(branchesPath + File.separator + "HEAD"), StandardCharsets.UTF_8);

        // iterate all the files in branches directory, and add remote branches files to each one
        // also remove every RB from being local branch, except for the Head
        for (File f : localBranchesFiles)
        {
            if ( !f.getName().equals("HEAD") && !f.getName().equals("commits") && !f.isDirectory())
            {
                // the new file for the remote branch
                String branchFilePath = remoteDirPath + File.separator + f.getName();
                File branchFile = new File(branchFilePath);
                branchFile.createNewFile();
                String localBranchSha1 = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                FilesOperations.writeTextToFile(localBranchSha1, branchFilePath);

                // delete every branch we copied from the RR, that is not the head, because we don't want to have
                // a local branch for each RB, only RB + RTB fir the head.
                if (!f.getName().equals(headBranchName))
                {
                    f.delete();
                }
            }
        }
    }

    private void createRBDirectory(String remoteDirPath)
    {
        File remoteDir = new File(remoteDirPath);
        // delete dir if we copied from the clones repo (in case the remote is also a cloning)
        if (remoteDir.exists())
        {
            FilesOperations.deleteFolder(remoteDir);
        }
        remoteDir.mkdirs();
    }

    // creates the RTB file, and write to it the head which should track the head of the remote
    // In general, the RTB file has couples of -LB-RB names, that follows each other
    public void createHeadRemoteTrackingBranchFile(String repoPath, String remoteRepoName) throws IOException
    {
        String remoteTrackingBranchesFilePath = repoPath + File.separator + ".magit" + File.separator +"RTB";
        createRTBFile(remoteTrackingBranchesFilePath);
        // on cloning, we need head of the remote, to be tracked by our local branch
        // and our HEAD should point to the relevant LB
        String headName = FileUtils.readFileToString(new File(repoPath + File.separator + ".magit" +
                File.separator + "branches" + File.separator + "HEAD"), StandardCharsets.UTF_8);


        String strForRTB = headName + System.lineSeparator()
                + remoteRepoName + File.separator + headName
                + System.lineSeparator() + ";" + System.lineSeparator();
        // write the head to RTB
        FilesOperations.writeTextToFile(strForRTB, remoteTrackingBranchesFilePath);
    }

    public void createRTBFile(String remoteTrackingBranchesFilePath) throws IOException
    {
        File rtbFile = new File(remoteTrackingBranchesFilePath);
        // delete dir if we copied from the clones repo (in case the remote is also a cloning)
        if (rtbFile.exists())
            rtbFile.delete();
        rtbFile.createNewFile();
    }

    public void setRemoteRepoPath(String remoteRepoPath) {
        this.remoteRepoPath = remoteRepoPath;
    }

    public void initializeRemoteFileAndRemotePath(MagitRepository magitRepository, String remoteRepoName) throws Exception
    {
        if (remoteRepoPath == null)
        {
            // set remote path, and create remote file
            MagitRepository.MagitRemoteReference remoteRepo = magitRepository.getMagitRemoteReference();
            remoteRepoPath = remoteRepo.getLocation();
            writeRemoteNameAndPathToFile(remoteRepoPath, repositoryLocation, remoteRepoName);
        }

        //initializing all branches list
        resetAndUpdateAllOfTheBranches();
        //Create commit tree
        initializeMagitCommitTree();
    }

    public void writeRemoteNameAndPathToFile(String remotePath, String pathOfNewRepo, String remoteRepoName) throws IOException
    {
        // creating two files: remote, and remote-name, which have the remote path and the remote name
        String remoteFilePath = pathOfNewRepo + File.separator + ".magit" + File.separator +  "remote";
        String remoteNameFilePath = pathOfNewRepo + File.separator + ".magit" + File.separator + "remote-name";

        File remoteFile = new File(remoteFilePath);
        File remoteNameFile = new File(remoteNameFilePath);
        // delete file if we copied from the clones repo (in case the remote is also a cloning)
        if (remoteFile.exists())
            remoteFile.delete();
        if (remoteNameFile.exists())
            remoteNameFile.delete();

        remoteFile.createNewFile();
        remoteNameFile.createNewFile();

        FilesOperations.writeTextToFile(remotePath, remoteFilePath);
        FilesOperations.writeTextToFile(remoteRepoName, remoteNameFilePath);
    }

    public static Boolean isRemoteMagitRepo(MagitRepository magitRepo)
    {
        return (magitRepo.getMagitRemoteReference() != null
                && magitRepo.getMagitRemoteReference().getLocation() != null
                && magitRepo.getMagitRemoteReference().getName() != null);
    }
    public void setRepositoryLogicalNameForUser(String repositoryLogicalNameForUser) {
        this.repositoryLogicalNameForUser = repositoryLogicalNameForUser;
    }

    public Branch getBranchByName(String branchName) {
        for(Branch branch : allOfTheBranches){
            if(branch.getName().equals(branchName)){
                return branch;
            }
        }
        return null;
    }

    public String getRemoteRepoPath() {
        return remoteRepoPath;
    }

    public String getRemoteRepoName() {
        return remoteRepoName;
    }

    public void addNewBranchOnSpecificCommit(Branch newBranch, Commit commit) {

        allOfTheBranches.add(newBranch);
        if (commit != null && !commit.getSha1().equals("null"))
        {
            addNewBranchToTreeWhenCommitExist(newBranch.getName(), commit.getSha1());
        }
    }

    public Commit getLastCommit()
    {
        MagitCommitNode lastNode = commitTree.getLastCommitNode();
        if (lastNode != null && lastNode.getCommit() != null)
            return lastNode.getCommit();
        return null;
    }

    public void setCommitTree(MagitCommitTree commitTree) {
        this.commitTree = commitTree;
    }

    public void setRemoteRepoName(String remoteRepoName) {
        this.remoteRepoName = remoteRepoName;
    }

    public void setRemoteRepoUsername(String remoteRepoUsername) {
        this.remoteRepoUsername = remoteRepoUsername;
    }

    public String getRemoteRepoUsername() {
        return remoteRepoUsername;
    }
}