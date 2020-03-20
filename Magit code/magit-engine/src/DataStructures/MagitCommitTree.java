package DataStructures;

import magitObjects.Commit;

import java.text.SimpleDateFormat;
import java.util.*;


public class MagitCommitTree {

    // Sha1<->MagitCommitNode dictionary, is used to avoid duplicated nodes in the tree
    private Dictionary<String, MagitCommitNode> sha1ToNodesDictionary;
    private MagitCommitNode root;


    public MagitCommitTree() {
        // initialize the dictionary of nodes
        sha1ToNodesDictionary = new Hashtable<>();
    }

    public MagitCommitNode getRoot() {
        return root;
    }

    public void setRoot(MagitCommitNode root) {
        this.root = root;
    }

    public void addNodeToDictionary(String sha1, MagitCommitNode node) {
        sha1ToNodesDictionary.put(sha1, node);
    }

    public boolean isCommitSha1InTree(String sha1) {
        if (sha1ToNodesDictionary.get(sha1) == null)
            return false;
        else
            return true;
    }

    public Dictionary<String, MagitCommitNode> getSha1ToNodesDictionary() {
        return sha1ToNodesDictionary;
    }

    public MagitCommitNode getLastCommitNode()
    {
        int x =1;
        List<MagitCommitNode> listOfMagitCommitNodes = Collections.list(sha1ToNodesDictionary.elements());
        if (!listOfMagitCommitNodes.isEmpty())
        {
            MagitCommitNode lastMagitCommit = Collections.min(listOfMagitCommitNodes, dateComparator);
            return lastMagitCommit;
        }
        else
            return null;
    }

    public static Comparator<MagitCommitNode> dateComparator = new Comparator<MagitCommitNode>() {
        public int compare(MagitCommitNode node1, MagitCommitNode node2) {
            Commit commit1 = node1.getCommit();
            Commit commit2 = node2.getCommit();
            try {

                // we parse the date string into a date and compare the dates
                String dateOfFirst = commit1.getmCreatedDate();
                String dateOfSecond = commit2.getmCreatedDate();
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

                Date dateOfFirstDate = format.parse(dateOfFirst);
                Date dateOfSecondDate = format.parse(dateOfSecond);
                return (dateOfSecondDate.compareTo(dateOfFirstDate));
            } catch (Exception e) {
                return (commit2.getmCreatedDate().compareTo(commit1.getmCreatedDate()));
            }
        }
    };
}