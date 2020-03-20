package DataStructures;

import magitObjects.GitObject;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    private GitObject mNode;
    private List<TreeNode> mChildren;
    private TreeNode mParent;

    public TreeNode(GitObject mNode, List<TreeNode> mChildren, TreeNode mParent) {
        this.mNode = mNode;
        this.mChildren = mChildren;
        this.mParent = mParent;
    }



    public void addChild(TreeNode child) {

        if (mChildren == null){
            mChildren = new ArrayList<>();
        }

        mChildren.add(child);
    }

    public GitObject getmNode() {
        return mNode;
    }

    public void setmNode(GitObject mNode) {
        this.mNode = mNode;
    }

    public List<TreeNode> getmChildren() {
        return mChildren;
    }

    public void setmChildren(List<TreeNode> mChildren) {
        this.mChildren = mChildren;
    }

    public TreeNode getmParent() {
        return mParent;
    }

    public void setmParent(TreeNode mParent) {
        this.mParent = mParent;
    }
}
