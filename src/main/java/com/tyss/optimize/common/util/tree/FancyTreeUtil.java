package com.tyss.optimize.common.util.tree;

import com.tyss.optimize.common.util.CommonConstants;
import com.tyss.optimize.common.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
@Slf4j
public class FancyTreeUtil {

    public static String buildFancyTree(List<Document> documentList, String rootId, String key, boolean sort) {

        StringBuilder fancyTreeBuilder = new StringBuilder();

        flattenTree(documentList, key);
        if(sort) {

            List<Document> scrList;
            scrList = documentList.stream().filter(document -> document.getString("_id").contains("SCR")).collect(Collectors.toList());
            documentList.removeAll(scrList);
            documentList.addAll(scrList);

            List<Document> preOrderList;
            preOrderList = documentList.stream().filter(document -> document.getString("_id").contains("PRE_")).collect(Collectors.toList());
            documentList.removeAll(preOrderList);
            documentList.addAll(preOrderList);

            List<Document> postConList;
            postConList = documentList.stream().filter(document -> document.getString("_id").startsWith("PE_POST")).collect(Collectors.toList());
            documentList.removeAll(postConList);

            List<Document> postOrderList;
            List<Document> postOrderParentList;

            postOrderList = documentList.stream().filter(document -> document.getString("_id").contains("POST_")).collect(Collectors.toList());
            documentList.removeAll(postOrderList);

            postOrderParentList = postOrderList.stream().filter(document -> document.getString("_id").startsWith("POST_MOD")).collect(Collectors.toList());
            postOrderList.removeAll(postOrderParentList);

            documentList.addAll(postOrderParentList);
            documentList.addAll(postOrderList);

            log.info("\n---------  Before  ---------- \n");
            documentList.stream().forEach(d -> log.info(d.getString("_id") + " = " + d.getString("parentId") + " = " + d.getInteger("hierarchy") + " = " + d.getDouble("executionOrder") + " = " + d.getString("name")));
            Collections.sort(documentList, new SortByExecutionOrder());
            documentList.addAll(postConList);

            log.info("\n----------  After ---------- \n ");

            documentList.stream().forEach(d -> log.info(d.getString("_id") + " = " + d.getString("parentId") + " = " + + d.getInteger("hierarchy") + " = " + d.getDouble("executionOrder") + " = " + d.getString("name")));

            List<Document> reOrderedList = reOrderParent(documentList, rootId);
            log.info("\n ------------  reOrderedList ---------- \n");
            reOrderedList.stream().forEach(d -> log.info(d.getString("_id") + " = " + d.getString("parentId") + " = " + + d.getInteger("hierarchy") + " = " + d.getDouble("executionOrder") + " = " + d.getString("name")));
            fancyTreeBuilder.append("[");
            convertToFancyTree(fancyTreeBuilder, constructTree(reOrderedList, rootId));
        }
        else {
            fancyTreeBuilder.append("[");
            convertToFancyTree(fancyTreeBuilder, constructFolderSortedTree(documentList, rootId, key));
        }

        fancyTreeBuilder.append("]");
        String treeJson = fancyTreeBuilder.toString().replace("_id", "key").
         //       replace("\"name\"", "\"title\"").
                replace("},children",", \"children\"")
                .replace(",]", "]");
        // treeJson = treeJson.replace("\"" , "'");
        return treeJson;

    }

    public static void sortDocument(List<Document> documentList) {
        Collections.sort(documentList, new SortByExecutionOrder());
    }

    private static void flattenTree(List<Document> documentList, String key) {

        List<Document> keyDocumentList = new ArrayList();

        documentList.forEach( (document) ->
        {
            document.put("modifiedOn", CommonUtil.getFormattedDate(String.valueOf(document.get("modifiedOn"))));
            document.put("title", document.get("name"));

            if(CommonConstants.SCRIPTS.equalsIgnoreCase(key) && CommonConstants.ROOT.equalsIgnoreCase( document.getString("name")))
            {
                document.put("title", "Root Module");
                document.put("name", "Root Module");
            }

            if(Objects.nonNull(document.get("conditions"))) {

                List<Document> conditionsDocument = (List<Document>) document.get("conditions");
                Document preConditionFolder = new Document();
                Document postConditionFolder = new Document();
                String parentId = (String) document.get("_id");
                String parentName = document.getString("name");
                Integer hierarchy = document.getInteger("hierarchy");

                AtomicReference<Boolean> isPreConditionsAvailable = new AtomicReference<>(false);
                AtomicReference<Boolean> isPostConditionsAvailable = new AtomicReference<>(false);
                preConditionFolder.put("title", "Preconditions");
                preConditionFolder.put("key", "PRE_" + parentId);
                preConditionFolder.put("_id", "PRE_" + parentId);
                preConditionFolder.put("folder", true);
                preConditionFolder.put("parentId", parentId);
                preConditionFolder.put("parentName", parentName);
                preConditionFolder.put("executionOrder", 0.0);
                preConditionFolder.put("hierarchy", hierarchy+1);
                preConditionFolder.put("name", "Preconditions");
                preConditionFolder.put("type", "PRE");

                postConditionFolder.put("title", "Postconditions");
                postConditionFolder.put("key", "POST_" + parentId);
                postConditionFolder.put("_id", "POST_" + parentId);
                postConditionFolder.put("folder", true);
                postConditionFolder.put("parentId", parentId);
                postConditionFolder.put("parentName", parentName);
                postConditionFolder.put("executionOrder", 9999.0);
                postConditionFolder.put("hierarchy", hierarchy+1);
                postConditionFolder.put("name", "Postconditions");
                postConditionFolder.put("type", "POST");

                conditionsDocument.stream().forEach(condition -> {
                    String conditionType = (String) condition.get("type");
                    condition.put("modifiedOn", CommonUtil.getFormattedDate(String.valueOf(condition.get("modifiedOn"))));
                    condition.put("title", condition.get("stepGroupName"));

                    if ("PRE".equalsIgnoreCase(conditionType)) {
                        isPreConditionsAvailable.set(true);
                        condition.put("parentId", "PRE_" + parentId);
                    } else {
                        isPostConditionsAvailable.set(true);
                        condition.put("parentId", "POST_" + parentId);
                    }
                });
                if (!CollectionUtils.isEmpty(conditionsDocument)) {
                    conditionsDocument.stream().forEach(condition -> {
                        String statusType = (String) condition.get("status");
                        if ("enabled".equalsIgnoreCase(statusType)) {
                            if (isPreConditionsAvailable.get()) {
                                keyDocumentList.add(preConditionFolder);
                            }

                            if (isPostConditionsAvailable.get()) {
                                keyDocumentList.add(postConditionFolder);
                            }
                            keyDocumentList.add(condition);
                        }
                    });
                    document.remove("conditions");
                }
            }
            if(Objects.nonNull(document.get(key))) {
                List<Document> childDocs = (List<Document>) document.get(key);
                childDocs.forEach((childDoc) ->
                {
                    childDoc.put("modifiedOn", CommonUtil.getFormattedDate(String.valueOf(childDoc.get("modifiedOn"))));
                    childDoc.put("parentId",document.get("_id"));
                    childDoc.put("title", childDoc.get("name"));
                    keyDocumentList.add(childDoc);
                });
                document.remove(key);
            }

        });
        documentList.addAll(keyDocumentList);

    }


    private static void convertToFancyTree(StringBuilder fancyTreeBuilder, TreeNode<Document>  documentParent) {
        int count =  documentParent.children.size();

        for (int i = 0; i < count ; i++) {

            TreeNode<Document> child = documentParent.children.get(i);
            fancyTreeBuilder.append(child.data.toJson()).append(",");

            if(child.children.size() > 0)
            {
                fancyTreeBuilder.append("children: [ ");
                convertToFancyTree(fancyTreeBuilder, child);
                if(i == count - 1) {
                    fancyTreeBuilder.append("] } ");
                }else
                {
                    fancyTreeBuilder.append("] }, ");
                }
            }

        }

    }

    private static List<Document> reOrderParent(List<Document> documentList, String rootId) {
        log.info("reOrderParent Start");

        List<Document> reOrderedDocumentList = new ArrayList<>();
        int docSize = documentList.size();
        List<String> selectIdList = new ArrayList<>();

        for (int i = 0; i < docSize; i++) {
            Document doc = documentList.get(i);
            selectIdList.add(String.valueOf(doc.get("_id")));
            String parentId = String.valueOf(doc.get("parentId"));

            if (!selectIdList.contains(parentId)) {
                if (!CommonConstants.ROOT.equalsIgnoreCase(String.valueOf(doc.get("name")))) {
                    if (StringUtils.isEmpty(rootId)) {

                        Optional<Document> parentDocOptional = documentList.stream().filter(parentDoc -> parentDoc.getString("_id").
                                equals(doc.getString("parentId"))).findFirst();
                        if (parentDocOptional.isPresent()) {
                            Document parentDoc = parentDocOptional.get();

                            if (!reOrderedDocumentList.contains(parentDoc)) {
                                reOrderedDocumentList.add(parentDoc);
                            }
                        }
                    } else {
                        if (!rootId.equalsIgnoreCase(String.valueOf(doc.get("_id")))) {
                            Optional<Document> parentDocOptional = documentList.stream().filter(parentDoc -> parentDoc.getString("_id").
                                    equals(doc.getString("parentId"))).findFirst();
                            if (parentDocOptional.isPresent()) {
                                Document parentDoc = parentDocOptional.get();

                                if (!reOrderedDocumentList.contains(parentDoc)) {
                                    reOrderedDocumentList.add(parentDoc);
                                }
                            }
                        }
                    }
                }
            }

            if (!reOrderedDocumentList.contains(doc)) {
                reOrderedDocumentList.add(doc);
            }
        }

        List<Document> pepostConList;
        pepostConList = reOrderedDocumentList.stream().filter(document -> document.getString("_id").startsWith("PE_POST")).collect(Collectors.toList());
        reOrderedDocumentList.removeAll(pepostConList);
        reOrderedDocumentList.addAll(pepostConList);

        Collections.sort(reOrderedDocumentList,new SortByExecutionOrder());

        reOrderedDocumentList.stream().forEach(document -> {
            if (Objects.nonNull(document.get("hierarchy")) && Objects.nonNull(document.get("executionOrder")) ) {
                String execOrder = String.valueOf(document.get("executionOrder"));
                if (execOrder.length() == 3) {
                    execOrder = 0 + execOrder;
                }
                String hierarchy = String.valueOf(document.get("hierarchy"));
                hierarchy = hierarchy + 0;
                String increaseExOrder = hierarchy + execOrder;
                double exOrder = Double.parseDouble(increaseExOrder);
                document.put("executionOrder", exOrder);
            }
        });

        Collections.sort(reOrderedDocumentList,new SortByExecutionOrder());

        reOrderedDocumentList.stream().forEach(document -> {
                if (Objects.nonNull(document.get("hierarchy")) && Objects.nonNull(document.get("executionOrder")) && !document.getInteger("hierarchy").equals(0)) {
                    String hierarchy = String.valueOf(document.get("hierarchy"));
                    String execOrder = String.valueOf(document.get("executionOrder"));
                    int h = hierarchy.length();
                    int e = execOrder.length();
                    String exOrder = execOrder.substring(h, e - 1);
                    double initialExOrder = Double.parseDouble(exOrder);
                    document.put("executionOrder", initialExOrder);
                }
        });

        List<Document> postOrderList;
        List<Document> postOrderParentList;

        postOrderList = reOrderedDocumentList.stream().filter(document -> document.getString("_id").contains("POST_")).collect(Collectors.toList());
        reOrderedDocumentList.removeAll(postOrderList);

        postOrderParentList = postOrderList.stream().filter(document -> document.getString("_id").startsWith("POST_MOD")).collect(Collectors.toList());
        postOrderList.removeAll(postOrderParentList);

        reOrderedDocumentList.addAll(postOrderParentList);
        reOrderedDocumentList.addAll(postOrderList);
        return reOrderedDocumentList;
    }


    private static TreeNode<Document> constructTree(List<Document> documentList, String rootId) {

        Set<String> fancyTreeFolderSet = new HashSet(CommonConstants.fancyTreeFolderList);
        TreeNode<Document> root = new TreeNode<Document>(new Document());
        {
            documentList.forEach( (document) -> {
                String docType = document.get("_id").toString().substring(0 , 3);
                if(document.get("_id").equals(rootId) || Objects.isNull(document.get("parentId")))
                {
                    root.addChild(document);
                }
                else
                {
                    TreeNode<Document> parent = findDocumentParent(root, document);
                    if(Objects.nonNull(parent))
                    {
                        parent.addChild(document);
                        String key = Objects.nonNull(parent.data.get("key")) ? parent.data.get("key").toString() : null;
                        if(Objects.nonNull(key) && (key.startsWith("PRE_") || key.startsWith("POST_"))){
                            parent.data.put("scriptCount",parent.children.size());
                        }
                    }
                }
                if(fancyTreeFolderSet.contains(docType))
                {
                    document.put("folder", true);
                    long moduleCount = documentList.stream().filter(doc -> !document.getString("_id").equals(doc.getString("_id")) && Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && Objects.isNull(doc.get("type"))).count();
                    document.put("subModuleCount", (int) moduleCount);
                    long scriptCount = documentList.stream().filter(doc -> Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && Objects.nonNull(doc.get("type")) && doc.get("type").equals(CommonConstants.script)).count();
                    document.put("scriptCount", (int) scriptCount);
                }
            });
        }
        return root;
    }

    private static TreeNode<Document> constructFolderSortedTree(List<Document> documentList, String rootId, String key) {
        Set<String> fancyTreeFolderSet = new HashSet(CommonConstants.fancyTreeFolderList);
        Map<String, String> keyMap = getCountKey(key);
        TreeNode<Document> root = new TreeNode<Document>(new Document());
        {
            documentList.forEach( (document) -> {
                if(document.get("_id").equals(rootId) || Objects.isNull(document.get("parentId")))
                {
                    root.addChild(document);
                    String docType = document.get("_id").toString().substring(0 , 3);
                    if(fancyTreeFolderSet.contains(docType))
                    {
                        document.put("folder", true);
                        updateTreeCount(documentList, document, keyMap);
                    }
                }
                else
                {
                    TreeNode<Document> parent = findDocumentParent(root, document);
                    if(Objects.nonNull(parent))
                    {
                        String docType = document.get("_id").toString().substring(0 , 3);
                        if(fancyTreeFolderSet.contains(docType))
                        {
                            document.put("folder", true);
                            parent.addChild(document);
                            updateTreeCount(documentList, document, keyMap);
                        }
                        else {
                            parent.addInnerChild(document);
                        }
                    }
                }
            });
        }
        return root;
    }

    private static void updateTreeCount(List<Document> documentList, Document document, Map<String, String> keyMap) {
        long folderCount = documentList.stream().filter(doc -> !document.getString("_id").equals(doc.getString("_id")) && Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && doc.getBoolean("folder")).count();
        document.put(keyMap.get("folderKey"), (int) folderCount);
        long childCount = 0;
        if(keyMap.get("childKey").equalsIgnoreCase("elementCount")){
            childCount = documentList.stream().filter(doc -> Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && !doc.getBoolean("folder") && (Objects.isNull(doc.get("isShared")) || !doc.get("isShared").toString().equals("Y"))).count();
        } else{
            childCount = documentList.stream().filter(doc -> Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && !doc.getBoolean("folder")).count();
        }
        document.put(keyMap.get("childKey"), (int) childCount);
    }

    private static Map<String, String> getCountKey(String key){
        Map<String, String> keyMap = new HashMap<>();
        switch (key.toLowerCase()){
            case "elements" :
                keyMap.put("folderKey","subPageCount");
                keyMap.put("childKey","elementCount");
                break;
            case "scripts" :
                keyMap.put("folderKey","subModuleCount");
                keyMap.put("childKey","scriptCount");
                break;
            case "programelements" :
                keyMap.put("folderKey","subPackageCount");
                keyMap.put("childKey","programElementCount");
                break;
            case "stepgroups" :
                keyMap.put("folderKey","subLibraryCount");
                keyMap.put("childKey","stepGroupCount");
                break;
            case "files" :
            default:
                keyMap.put("folderKey","subFolderCount");
                keyMap.put("childKey","fileCount");
                break;
        }
        return keyMap;
    }

    private static TreeNode<Document>  findDocumentParent(TreeNode<Document> treeRoot, Document document) {

        Comparable<Document> searchCriteria = new Comparable<Document>() {
            @Override
            public int compareTo(Document treeData) {
                if (treeData.get("_id") == null)
                    return 1;
                boolean nodeOk = treeData.get("_id").equals(document.get("parentId"));
                return nodeOk ? 0 : 1;
            }
        };
        TreeNode<Document> found = treeRoot.findTreeNode(searchCriteria);

        return found;
    }

}
