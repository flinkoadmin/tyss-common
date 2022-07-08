package com.tyss.optimize.common.util.tree;

import com.tyss.optimize.common.util.CommonConstants;
import com.tyss.optimize.common.util.MongoCollections;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Slf4j
public class SampleTreeData {

    public static void main(String[] args) {

        executionSortTest();

    }

    private static void executionSortTest() {

        List<Document> documentList = new ArrayList<>();

        Document doc1 = new Document();
        doc1.put("_id", 1);
        doc1.put("executionOrder", 2);
        documentList.add(doc1);

        Document doc2 = new Document();
        doc2.put("_id", 2);
        doc2.put("executionOrder", 3);
        documentList.add(doc2);

        Document doc3 = new Document();
        doc3.put("_id", 3);
        doc3.put("executionOrder", 1);

        documentList.add(doc3);

        Collections.sort(documentList, new SortByExecutionOrder());

        log.info(String.valueOf(documentList));
    }

    public static void testReOrderParent(String[] args) {

        List<Document> documentList = new ArrayList<>();

        Document doc1 = new Document();
        doc1.put("_id", 1);
        doc1.put("name", "root");
        doc1.put("parentId", null);
        doc1.put("executionOrder", 0.0);
        documentList.add(doc1);

        Document doc2 = new Document();
        doc2.put("_id", 2);
        doc2.put("name", "ele2");
        doc2.put("executionOrder", 0.0);
        doc2.put("parentId", 1);
        documentList.add(doc2);

        Document doc3 = new Document();
        doc3.put("_id", 3);
        doc3.put("name", "ele3");
        doc3.put("executionOrder", 3.0);
        doc3.put("parentId", 5);
        documentList.add(doc3);

        Document doc4 = new Document();
        doc4.put("_id", 4);
        doc4.put("name", "ele4");
        doc4.put("executionOrder", 2.0);
        doc4.put("parentId", 3);
        documentList.add(doc4);

        Document doc5 = new Document();
        doc5.put("_id", 5);
        doc5.put("name", "ele5");
        doc5.put("executionOrder", 5.0);
        doc5.put("parentId", 4);
        documentList.add(doc5);

        Collections.sort(documentList, new SortByExecutionOrder());

        log.info("Before" + documentList);
        reOrderStaticParent( documentList, -1);

    }

    public static void reOrderStaticParent(List<Document> documentList, int lastCount) {

        log.info("Start ReOrder  = ");
        int docSize = documentList.size();
        List<Document> orphanDocumentList = new ArrayList<>();
        List<String> selectIdList = new ArrayList<>();
        for (int i = 0; i < docSize ; i++) {
            Document doc = documentList.get(i);
            selectIdList.add(String.valueOf(doc.get("_id")));
            String parentId = String.valueOf(doc.get("parentId"));
            log.info("_id = " + String.valueOf(doc.get("_id")));
            log.info("parentId = " + parentId);

            if(!selectIdList.contains(parentId))
            {
                if(! CommonConstants.ROOT.equalsIgnoreCase(String.valueOf(doc.get("name")))) {
                    orphanDocumentList.add(doc);
                    log.info("InsideOrphan = " + String.valueOf(doc.get("_id")));

                }
            }
            log.info("");
        }
        if(!CollectionUtils.isEmpty(orphanDocumentList)) {
            documentList.removeAll(orphanDocumentList);
            documentList.addAll(orphanDocumentList);
            if(lastCount != orphanDocumentList.size()) {
                reOrderStaticParent(documentList, orphanDocumentList.size());
            }

        }

        log.info("After" + documentList);
    }

    public static TreeNode<String> getSet1() {
        TreeNode<String> root = new TreeNode<String>("root");
        {
            TreeNode<String> node0 = root.addChild("node0");
            TreeNode<String> node1 = root.addChild("node1");
            TreeNode<String> node2 = root.addChild("node2");
            {
                TreeNode<String> node21 = node2.addChild("node21");
                {
                    TreeNode<String> node210 = node21.addChild("node210");
                    TreeNode<String> node211 = node21.addChild("node211");
                }
            }
            TreeNode<String> node3 = root.addChild("node3");
            {
                TreeNode<String> node30 = node3.addChild("node30");
            }
        }

        return root;
    }

    public static TreeNode<String> getSetSOF() {
        TreeNode<String> root = new TreeNode<String>("root");
        {
            TreeNode<String> node0 = root.addChild("node0");
            TreeNode<String> node1 = root.addChild("node1");
            TreeNode<String> node2 = root.addChild("node2");
            {
                TreeNode<String> node20 = node2.addChild(null);
                TreeNode<String> node21 = node2.addChild("node21");
                {
                    TreeNode<String> node210 = node20.addChild("node210");
                }
            }
        }

        return root;
    }
}