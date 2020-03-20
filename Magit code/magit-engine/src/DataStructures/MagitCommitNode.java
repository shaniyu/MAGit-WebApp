package DataStructures;

import magitObjects.Commit;

import java.util.ArrayList;

public class MagitCommitNode {

    private Commit mNode;
    private MagitCommitNode firstParent;
    private MagitCommitNode secondParent;
    private ArrayList<String> branchesThatHasMe;

    public MagitCommitNode(Commit mNode) {
        this.mNode = mNode;
        this.firstParent = null;
        this.secondParent = null;
        branchesThatHasMe = new ArrayList<>();
    }

    public void setFirstParent(MagitCommitNode firstParent) {
        this.firstParent = firstParent;
    }

    public void setSecondParent(MagitCommitNode secondParent) {
        this.secondParent = secondParent;
    }

    public Commit getCommit() {
        return mNode;
    }


    public MagitCommitNode getFirstParent() {
        return firstParent;
    }

    public MagitCommitNode getSecondParent() {
        return secondParent;
    }

    public void addBranchToList(String branchName)
    {
        this.branchesThatHasMe.add(branchName);
    }

    public ArrayList<String> getBranchesThatHasMe() {
        return branchesThatHasMe;
    }

    public String getSha1OfNode()
    {
        if (mNode != null && !mNode.getSha1().equals("null"))
        {
            return mNode.getSha1();
        }
        else
            return null;
    }

    public void setBranchesThatHasMe(ArrayList<String> branchesThatHasMe) {
        this.branchesThatHasMe = branchesThatHasMe;
    }
}
