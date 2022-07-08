package com.tyss.optimize.common.util.tree;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SampleSearch {

    public static void main(String[] args) {

        Comparable<String> searchCriteria = new Comparable<String>() {
            @Override
            public int compareTo(String treeData) {
                if (treeData == null)
                    return 1;
                boolean nodeOk = treeData.contains("210");
                return nodeOk ? 0 : 1;
            }
        };

        TreeNode<String> treeRoot = SampleTreeData.getSet1();
        TreeNode<String> found = treeRoot.findTreeNode(searchCriteria);

       log.info("Found: " + found);
    }

}