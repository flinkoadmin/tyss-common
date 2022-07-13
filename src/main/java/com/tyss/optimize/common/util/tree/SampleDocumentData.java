package com.tyss.optimize.common.util.tree;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
@Slf4j
public class SampleDocumentData {

    static StringBuilder fancyTreeBuilder = new StringBuilder();

    static Set<String> fancyTreeFolderSet = new HashSet(Arrays.asList("PAG", "LIB", "MOD"));

    public static void main(String[] args) {

        List<Document> documentList = new ArrayList<>();


        List<Document> elementPage1List = new ArrayList<>();

        Document eleDocument = new Document();
        eleDocument.append("_id","ELE1001").append("name","element1");

        Document eleDocument2 = new Document();
        eleDocument2.append("_id","ELE1002").append("name","element2");

        elementPage1List.add(eleDocument);
        elementPage1List.add(eleDocument2);

        Document document = new Document();
        document.append("_id","PAG1001").append("name","page1");
        document.append("elements",elementPage1List);
        documentList.add(document);

        Document document2 = new Document();
        document2.append("_id","PAG1002").append("name","page2");
        documentList.add(document2);

        Document document3 = new Document();
        document3.append("_id","PAG1003").append("name","page3").append("parentPageId","PAG1001");
        documentList.add(document3);

        Document document4 = new Document();
        document4.append("_id","PAG1004").append("name","page4").append("parentPageId","PAG1001");
        documentList.add(document4);
        Instant start = Instant.now();

      //  buildFancyTree(documentList, "elements");

       String fancyTreeJson = FancyTreeUtil.buildFancyTree(documentList, "page4", "elements", false, null);
        Instant finish = Instant.now();

        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.valueOf(timeElapsed));
        log.info(fancyTreeJson);

    }


    public static String buildFancyTree(List<Document> documentList, String key) {


        flattenTree(documentList, key);
        fancyTreeBuilder.append("[");
        printFancyTree(constructTree(documentList));
        fancyTreeBuilder.append("]");
        String pageJson = fancyTreeBuilder.toString().replace("_id", "key").
                replace("name", "title").
                replace("},children",", \"children\"")
                .replace(",]", "]");

    //    pageJson = pageJson.replace("elements", "children");
        return pageJson;
    }

    private static void flattenTree(List<Document> documentList, String key) {

        List<Document> keyDocumentList = new ArrayList();

        documentList.forEach( (document) ->
        {
            if(Objects.nonNull(document.get(key))) {
                List<Document> childDocs = (List<Document>) document.get(key);
                childDocs.forEach((childDoc) ->
                {
                    childDoc.put("parentPageId",document.get("_id"));
                    keyDocumentList.add(childDoc);
                });
                document.remove(key);
            }
        });

        documentList.addAll(keyDocumentList);
    }


    private static TreeNode<Document> constructTree(List<Document> documentList) {
        TreeNode<Document> root = new TreeNode<Document>(new Document());
        {
            documentList.forEach( (document) -> {
              //  log.info(document.get("_id") + " " + document.get("parentPageId"));
                if(Objects.isNull(document.get("parentPageId")))
                {
                    root.addChild(document);
                }
                else
                {
                    TreeNode<Document> parent = findDocumentParent(root, document);
                    if(Objects.nonNull(parent))
                    {
                        parent.addChild(document);

                    }
                }
                String docType = document.get("_id").toString().substring(0 , 3);
                if(fancyTreeFolderSet.contains(docType))
                {
                    document.put("folder", true);
                }
                if(Objects.nonNull(document.get("elements")))
                {
                    document.put("children",document.get("elements"));
                    document.remove("elements");
                }
            });
        }
        return root;
    }

    public static TreeNode<Document>  findDocumentParent(TreeNode<Document> treeRoot, Document document) {

        Comparable<Document> searchCriteria = new Comparable<Document>() {
            @Override
            public int compareTo(Document treeData) {
                if (treeData.get("_id") == null)
                    return 1;
                boolean nodeOk = treeData.get("_id").equals(document.get("parentPageId"));
                return nodeOk ? 0 : 1;
            }
        };

        TreeNode<Document> found = treeRoot.findTreeNode(searchCriteria);

        return found;
    }


    public static void printFancyTree(TreeNode<Document>  documentParent) {

//        fancyTreeBuilder.append(documentParent.data.toJson());
//
//        fancyTreeBuilder.append("children: [ ");

       int count =  documentParent.children.size();

        for (int i = 0; i < count ; i++) {

            TreeNode<Document> child = documentParent.children.get(i);
           fancyTreeBuilder.append(child.data.toJson()).append(",");


            if(child.children.size() > 0)
            {

                fancyTreeBuilder.append("children: [ ");
                printFancyTree(child);
                if(i == count - 1) {
                    fancyTreeBuilder.append("] ");
                }else
                {
                    fancyTreeBuilder.append("] }, ");
                }
            }


        }

//        documentParent.children.forEach( (child) ->
//        {
//            fancyTreeBuilder.append(child.data.toJson()).append(",");
//                  //  replace("{","")
//                  //  .replace("}","")).append(",");
//
//            int count = child.children.size();
//
//            if(child.children.size() > 0)
//            {
//                fancyTreeBuilder.append("children: [ ");
//                printFancyTree(child);
//                fancyTreeBuilder.append("] ");
//
//            }
//        });

    }



    private static String createIndent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(' ');
        }
        return sb.toString();
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

    public  void oldmain(String[] args) {

        Document d1 = new Document();
        d1.put("folder" , true);
        d1.put("name", "login");

        Document d2 = new Document();
        d2.put("folder" , true);
        d2.put("name", "logout");

        Document d3 = new Document();
        d3.put("name", "Ele1");

        Document d4 = new Document();
        d4.put("name", "Ele2");

        Document d5 = new Document();
        d5.put("name", "Ele3");

        List<Document> dbList = new ArrayList<>();
        dbList.add(d1);
        dbList.add(d2);
        dbList.add(d3);
        dbList.add(d4);
        dbList.add(d5);

        LinkedList<Document> treeList = new LinkedList();


        log.info(String.valueOf(treeList));

        dbList.forEach(document -> {

            if(Objects.nonNull(document.get("folder")) && (Boolean) document.get("folder"))
            {
                treeList.add(document);

            }
            else
            {
                int itemIndex = getItemIndex(treeList);
                treeList.add(itemIndex, document);

            }
        });

        log.info(String.valueOf(dbList));

        log.info(String.valueOf(treeList));
    }

    private static int getItemIndex(LinkedList<Document> abc) {
        int listSize = abc.size();
        int folderIndex = 0;

        for (int i = 0; i < listSize; i++) {

            Document document = abc.get(i);

            if(Objects.nonNull(document.get("folder")) && (Boolean) document.get("folder"))
            {
                folderIndex = i;
            }
        }
        return folderIndex -1;
    }
}