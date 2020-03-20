package magitEngine;

import Exceptions.XmlNotValidException;
import magitObjects.Repository;
import xmlObjects.*;
import java.io.File;

public class XmlValidation {

    MagitRepository magitRepository;

    public XmlValidation(MagitRepository magitRepository) {
        this.magitRepository = magitRepository;
    }

    public boolean isThereANonRepositoryDirectoryInMagitRepoPath() {
        String finalRepoPathToCreateFromXml = magitRepository.getLocation();
        File dir = new File(finalRepoPathToCreateFromXml);

        return dir.exists() && !MagitEngine.isPathAlreadyARepo(finalRepoPathToCreateFromXml);
    }

    public boolean isMagitRepositoryValid() throws XmlNotValidException{
        boolean res = true;

        res =   checkMagitRemoteValidity()
                && checkMagitBlobsValidity()
                && checkMagitFoldersValidity()
                && checkMagitCommitsValidity()
                && checkMagitBranchesValidity();

        return res;
    }

    private boolean checkMagitRemoteValidity() throws XmlNotValidException{

        boolean res = true;

        if(Repository.isRemoteMagitRepo(magitRepository))
        {
            String remotePath = magitRepository.getMagitRemoteReference().getLocation();
            File remoteFolder = new File(remotePath);
            if(!remoteFolder.exists()){
                throw new XmlNotValidException(XmlStrings.REMOTE_REPOSITORY_ISNT_REPOSITORY1 + remotePath
                        + XmlStrings.REMOTE_REPOSITORY_DOESNT_EXIST);
            }
            else if(! MagitEngine.isPathAlreadyARepo(remotePath)){
                throw new XmlNotValidException(XmlStrings.REMOTE_REPOSITORY_ISNT_REPOSITORY1 + remotePath
                        + XmlStrings.REMOTE_REPOSITORY_ISNT_REPOSITORY2);
            }
        }

        return res;
    }

    public boolean isThereAlreadyRepoInMagitRepoPath() {
        String finalRepoPathToCreateFromXml = magitRepository.getLocation();
        File dir = new File(finalRepoPathToCreateFromXml);

        return dir.exists() && MagitEngine.isPathAlreadyARepo(finalRepoPathToCreateFromXml);
    }

    private boolean checkMagitBlobsValidity() throws XmlNotValidException{
        boolean res = true;

        //Checking that there aren't multiple blobs with the same id
        for(MagitBlob magitBlob : magitRepository.getMagitBlobs().getMagitBlob()){
            String magitBlobId = magitBlob.getId();
            for (MagitBlob magitBlob1 : magitRepository.getMagitBlobs().getMagitBlob()){
                if(magitBlob != magitBlob1 && magitBlobId.equals(magitBlob1.getId())){
                    throw new XmlNotValidException(XmlStrings.DUPLICATE_BLOB_ID + magitBlobId);
                }
            }
        }

        return res;
    }

    private boolean checkMagitFoldersValidity() throws XmlNotValidException{

        boolean res = true;

        //Checking that there aren't multiple folders with the same id
        for(MagitSingleFolder magitSingleFolder : magitRepository.getMagitFolders().getMagitSingleFolder()){
            String magitFolderId = magitSingleFolder.getId();
            for (MagitSingleFolder magitSingleFolder1 : magitRepository.getMagitFolders().getMagitSingleFolder()){
                if(magitSingleFolder != magitSingleFolder1 && magitFolderId.equals(magitSingleFolder1.getId())){
                    throw new XmlNotValidException(XmlStrings.DUPLICATE_FOLDER_ID + magitFolderId);
                }
            }
            //Checking that folders contain blob/ file ids that exist and don't contains the folder is itself
            for(Item item : magitSingleFolder.getItems().getItem()){
                //Checking that the folder doesn't contain itself
                if(item.getId().equals(magitSingleFolder.getId()) && item.getType().equals("folder")){
                    throw new XmlNotValidException(XmlStrings.FOLDER_HOLDS_ITSELF1 + magitSingleFolder.getId() + XmlStrings.FOLDER_HOLDS_ITSELF2);
                }
                //Checking that the folder doesn't folder/blob ids that don't exist
                if(!itemIDExists(item)){
                    throw new XmlNotValidException(XmlStrings.FOLDER_HOLDS_ID_THAT_DOESNT_EXIST1
                            + magitSingleFolder.getId() + XmlStrings.FOLDER_HOLDS_ID_THAT_DOESNT_EXIST2);
                }
            }
        }

        return res;
    }

    //This method checks if a specific item (blob or folder) id exists in the magitRepository object blobs/folders
    private boolean itemIDExists(Item item) {

        boolean res = false;

        if(item.getType().equals("blob")){
            for (MagitBlob magitBlob : magitRepository.getMagitBlobs().getMagitBlob()){
                if(item.getId().equals(magitBlob.getId())){
                    return true;
                }
            }
        }
        else{// type = folder
            for (MagitSingleFolder magitSingleFolder : magitRepository.getMagitFolders().getMagitSingleFolder()){
                if(item.getId().equals(magitSingleFolder.getId())){
                    return true;
                }
            }
        }

        return res;
    }

    private boolean checkMagitCommitsValidity() throws XmlNotValidException{

        boolean res = true;

        //Checking that there aren't multiple commits with the same id
        for(MagitSingleCommit magitSingleCommit : magitRepository.getMagitCommits().getMagitSingleCommit()){
            String magitSingleCommitId = magitSingleCommit.getId();
            for (MagitSingleCommit magitSingleCommit1 : magitRepository.getMagitCommits().getMagitSingleCommit()){
                if(magitSingleCommit != magitSingleCommit1 && magitSingleCommitId.equals(magitSingleCommit1.getId())){
                    throw new XmlNotValidException(XmlStrings.DUPLICATE_COMMIT_ID + magitSingleCommitId);
                }
            }
            //Checking that commit only points to folder id that exists and only to folder that is root folder
            if(!folderIdExistsAndIsRoot(magitSingleCommit)){
                return false;
            }
        }

        return res;
    }

    private boolean folderIdExistsAndIsRoot(MagitSingleCommit magitSingleCommit) throws XmlNotValidException{
        boolean res = true;
        boolean itemIDFound = false;
        String commitRootFolderId = magitSingleCommit.getRootFolder().getId();

        for(MagitSingleFolder magitSingleFolder : magitRepository.getMagitFolders().getMagitSingleFolder()){
            if(commitRootFolderId.equals(magitSingleFolder.getId())) {
                itemIDFound = true;
                if(!magitSingleFolder.isIsRoot()) {
                    throw new XmlNotValidException(XmlStrings.COMMIT_POINTS_TO_NON_ROOT_FOLDER1
                            + magitSingleCommit.getId() + XmlStrings.COMMIT_POINTS_TO_NON_ROOT_FOLDER2);
                }
            }
        }
        if(!itemIDFound){
            throw new XmlNotValidException(XmlStrings.COMMIT_POINTS_TO_FOLDER_THAT_DOESNT_EXIST1
                    + magitSingleCommit.getId() + XmlStrings.COMMIT_POINTS_TO_FOLDER_THAT_DOESNT_EXIST2);
        }
        return res;
    }

    private boolean checkMagitBranchesValidity() throws XmlNotValidException{
        boolean res = true;

        //Checking that head points to branch name that exists
        if(!headPointsToExistingBranch()){
            throw new XmlNotValidException(XmlStrings.HEAD_POINTS_TO_BRANCH_THAT_DOESNT_EXIST);
        }
        //Checking that every branch points to an existing commit
        for(MagitSingleBranch magitSingleBranch : magitRepository.getMagitBranches().getMagitSingleBranch()){
            try{
                if(!commitIDExists(magitSingleBranch)){
                    throw new XmlNotValidException(XmlStrings.BRANCH_POINTS_TO_COMMIT_THAT_DOESNT_EXIST1
                            + magitSingleBranch.getName() +XmlStrings.BRANCH_POINTS_TO_COMMIT_THAT_DOESNT_EXIST2);
                }
                if(magitSingleBranch.isTracking()){ //Need to check if the branch is tracking a branch that is remote
                    checkBranchTrackingValidity(magitSingleBranch);
                }
            }
            catch (XmlNotValidException e){
                throw e;
            }
        }

        return res;
    }

    private void checkBranchTrackingValidity(MagitSingleBranch magitSingleBranch) throws XmlNotValidException{

        for(MagitSingleBranch magitSingleBranch1 : magitRepository.getMagitBranches().getMagitSingleBranch()){
            //If the remote tracking branch is pointing to an existing remote branch in the xml then it's valid
            if(magitSingleBranch.getTrackingAfter().equals(magitSingleBranch1.getName())
                    && magitSingleBranch1.isIsRemote()){
                return;
            }
        }
        throw new XmlNotValidException("Branch " + magitSingleBranch.getName() + XmlStrings.TRACKING_BRANCH_NOT_POINT_TO_EXISTING_REMOTE_BRANCH);
    }

    private boolean headPointsToExistingBranch() throws XmlNotValidException{
        boolean res = false;

        String headBranch = magitRepository.getMagitBranches().getHead();
        for(MagitSingleBranch magitSingleBranch : magitRepository.getMagitBranches().getMagitSingleBranch()){
            if(headBranch.equals(magitSingleBranch.getName())){
                return true;
            }
        }

        return res;
    }

    private boolean commitIDExists(MagitSingleBranch magitSingleBranch) throws XmlNotValidException{

        String commitOfBranchId = magitSingleBranch.getPointedCommit().getId();

        //It is possible for a branch to not point to any commit- for example in an empty repository
        if(!commitOfBranchId.isEmpty()){
            for(MagitSingleCommit magitSingleCommit : magitRepository.getMagitCommits().getMagitSingleCommit()){
                if(commitOfBranchId.equals(magitSingleCommit.getId())){
                    return true;
                }
            }
        }
        //if commitOfBranchId is empty- the only scenario that allows this is if we're loading
        //an empty repository xml, in this case we need to make sure that there aren't any commits in the xml
        else if (!(magitRepository.getMagitCommits().getMagitSingleCommit().size() == 0)){
            throw new XmlNotValidException(XmlStrings.HEAD_EMPTY_COMMIT);
        }
        else{
            return true;
        }
        return false;
    }
}
