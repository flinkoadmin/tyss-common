package com.tyss.optimize.common.util.tree;

import com.tyss.optimize.common.util.CommonConstants;
import com.tyss.optimize.common.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class FancyTreeUtil {


    private static int fixedThreadPoolSize;

    private static int batchSize;

    public static String buildFancyTree(List<Document> initialDocumentList, String rootId, String key, boolean sort, String testCaseType, boolean resourceRequired, int fixedThreadPoolSize1, int batchSize1) {
        long startTime = System.currentTimeMillis();
        log.info("buildFancyTree started..");
        StringBuilder fancyTreeBuilder = new StringBuilder();
        fixedThreadPoolSize = fixedThreadPoolSize1;
        batchSize = batchSize1;
        CompletableFuture<List<CompletableFuture<List<Document>>>> completableFutureList = divide(initialDocumentList, key);
        fancyTreeBuilder.append("[");

        try {
            List<CompletableFuture<List<Document>>> flattenDocumentList = completableFutureList.get();
            ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
            CompletableFuture<StringBuilder> fancyTreeBuilderCompletableFuture;
            if (sort) {
                List<Document> futureModuleList = Collections.synchronizedList(new ArrayList<>());
                List<Document> futureScrList = Collections.synchronizedList(new ArrayList<>());
                List<Document> futurePreOrderList = Collections.synchronizedList(new ArrayList<>());
                List<Document> futurePostConList = Collections.synchronizedList(new ArrayList<>());
                List<Document> futurePostOrderList = Collections.synchronizedList(new ArrayList<>());
                List<Document> futurePostOrderParentList = Collections.synchronizedList(new ArrayList<>());

                getFutureDocument(flattenDocumentList, futureModuleList, futureScrList, futurePreOrderList, futurePostConList,
                        futurePostOrderList, futurePostOrderParentList);

                Thread.sleep(10);

                List<Document> documentList = new ArrayList<>();
                documentList.addAll(futureModuleList);
                documentList.addAll(futureScrList);
                documentList.addAll(futurePreOrderList);
                documentList.addAll(futurePostOrderParentList);
                documentList.addAll(futurePostOrderList);

                Collections.sort(documentList, new SortByExecutionOrder());
                documentList.addAll(futurePostConList);

                List<Document> reOrderedList = reOrderParent(documentList, rootId);

                Object id = "";
                List<Object> parentIds = new ArrayList<>();
                for (Document modules : reOrderedList
                ) {
                    Double lastExecutionOrder = 0.0;
                    id = modules.get("_id");
                    for (Document data1 : reOrderedList
                    ) {
                        if (id.equals(data1.get("parentId"))) {
                            parentIds.add(data1.get("parentId"));
                            String type = String.valueOf(data1.get("type"));
                            if (!type.equals("POST")) {
                                Double executionOrder1 = (Double) data1.get("executionOrder");
                                lastExecutionOrder = executionOrder1;
                            }
                        }
                    }
                    for (Object parentIds1 : parentIds
                    ) {
                        if (!parentIds1.equals(id) && Objects.nonNull(modules.get("type"))) {
                            lastExecutionOrder = 0.0;
                        }
                    }
                    if (Objects.isNull(modules.get("type"))) {
                        modules.put("lastExecutionOrder", lastExecutionOrder);
                    }
                }

                Thread.sleep(10);
                TreeNode<Document> documentTreeNode;
                if (resourceRequired) {
                    documentTreeNode = getFutureConstructTree(reOrderedList, rootId, testCaseType).get();
                } else {
                    documentTreeNode = getFutureConstructTreeWithoutResources(reOrderedList, rootId).get();
                }
                fancyTreeBuilderCompletableFuture = CompletableFuture.supplyAsync(() -> convertToFancyTree(fancyTreeBuilder, documentTreeNode, key), executor);
            } else {
                List<Document> documentList = new ArrayList<>();
                flattenDocumentList.forEach(listCompletableFuture -> {
                    try {
                        documentList.addAll(listCompletableFuture.get());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Exception in flattenDocumentList: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
                    }
                });

                TreeNode<Document> documentTreeNode = getFutureConstructFolderSortedTree(documentList, rootId, key, resourceRequired).get();
                for (TreeNode<Document> documentTreeNode1 : documentTreeNode
                ) {
                    Object id = "";
                    Document data = documentTreeNode1.data;
                    List<Object> parentIds = new ArrayList<>();
                    id = data.get("_id");
                    Double lastExecutionOrder = 0.0;
                    Object id1 = id;
                    data.get("parentId");
                    if (data.size() > 0) {
                        for (TreeNode<Document> document1 : documentTreeNode1
                        ) {
                            if (id.equals(document1.data.get("parentId"))) {
                                parentIds.add(document1.data.get("parentId"));
                                lastExecutionOrder = (Double) document1.data.get("executionOrder");
                            }
                        }
                        for (Object parentIds1 : parentIds
                        ) {
                            if (!id.equals(parentIds1)) {
                                lastExecutionOrder = 0.0;
                            }
                        }
                        if (Objects.nonNull(data.get("defaultLibrary"))) {
                            data.put("lastExecutionOrder", lastExecutionOrder);
                        }
                    }
                }
                fancyTreeBuilderCompletableFuture = CompletableFuture.supplyAsync(() -> convertToFancyTree(fancyTreeBuilder, documentTreeNode, key), executor);
            }
            fancyTreeBuilderCompletableFuture.get();
            executor.shutdown();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception in buildFancyTree: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
        }
        fancyTreeBuilder.append("]");

        String treeJson = fancyTreeBuilder.toString().replace("_id", "key").
                replace("},children", ", \"children\"")
                .replace(",]", "]");
        long endTime = System.currentTimeMillis();
        log.info("buildFancyTree ended... total execution time of buildFancyTree -> " + (endTime - startTime) + " milliSeconds");
        return treeJson;

    }

    @Async
    public static void getFutureDocument(List<CompletableFuture<List<Document>>> allresults, List<Document> futureModuleList, List<Document> futureScrList,
                                         List<Document> futurePreOrderList, List<Document> futurePostConList, List<Document> futurePostOrderList,
                                         List<Document> futurePostOrderParentList) {

        ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
        int docSize = batchSize;
        for (int start = 0; start <= allresults.size(); start += docSize) {

            int end = Math.min(start + docSize, allresults.size());
            List<CompletableFuture<List<Document>>> subList = allresults.subList(start, end);

            CompletableFuture<Object> fancyTreeBuilderCompletableFuture = CompletableFuture.supplyAsync(() -> orderListOnConditions(subList, futureModuleList, futureScrList, futurePreOrderList,
                    futurePostConList, futurePostOrderList, futurePostOrderParentList), executor);
            try {
                fancyTreeBuilderCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception in fancyTreeBuilderCompletableFuture: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
            }
        }
        executor.shutdown();
    }

    public static Object orderListOnConditions(List<CompletableFuture<List<Document>>> futureFlattenDocument, List<Document> futureModuleList, List<Document> futureScrList, List<Document> futurePreOrderList,
                                               List<Document> futurePostConList, List<Document> futurePostOrderList, List<Document> futurePostOrderParentList) {

        for (CompletableFuture<List<Document>> futureDoc : futureFlattenDocument) {
            try {
                List<Document> documentList = futureDoc.get();
                List<Document> scrList;

                List<Document> moduleList = documentList.stream().filter(document -> document.getString("_id").startsWith("MOD")).collect(Collectors.toList());
                futureModuleList.addAll(moduleList);

                scrList = documentList.stream().filter(document -> document.getString("_id").contains("SCR")).collect(Collectors.toList());
                futureScrList.addAll(scrList);


                List<Document> preOrderList;
                preOrderList = documentList.stream().filter(document -> document.getString("_id").contains("PRE_")).collect(Collectors.toList());
                futurePreOrderList.addAll(preOrderList);

                List<Document> postConList;
                postConList = documentList.stream().filter(document -> document.getString("_id").startsWith("PE_POST")).collect(Collectors.toList());
                documentList.removeAll(postConList);
                futurePostConList.addAll(postConList);

                List<Document> postOrderList;
                List<Document> postOrderParentList;

                postOrderList = documentList.stream().filter(document -> document.getString("_id").contains("POST_")).collect(Collectors.toList());

                postOrderParentList = postOrderList.stream().filter(document -> document.getString("_id").startsWith("POST_MOD")).collect(Collectors.toList());
                postOrderList.removeAll(postOrderParentList);

                futurePostOrderList.addAll(postOrderList);
                futurePostOrderParentList.addAll(postOrderParentList);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static void sortDocument(List<Document> documentList) {
        Collections.sort(documentList, new SortByExecutionOrder());
    }

    private static List<Document> flattenTree(List<Document> subDocumentList, String key) {

        List<Document> keyDocumentList = new ArrayList();
        List<Document> documentList = new ArrayList<>();
        documentList.addAll(subDocumentList);
        documentList.forEach(document -> {
            document.put("modifiedOn", CommonUtil.getFormattedDate(String.valueOf(document.get("modifiedOn"))));
            document.put("title", document.get("name"));

            if (CommonConstants.SCRIPTS.equalsIgnoreCase(key) && CommonConstants.ROOT.equalsIgnoreCase(document.getString("name"))) {
                document.put("title", "Root Module");
                document.put("name", "Root Module");
            }

            if (Objects.nonNull(document.get("conditions"))) {

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
                preConditionFolder.put("hierarchy", hierarchy + 1);
                preConditionFolder.put("name", "Preconditions");
                preConditionFolder.put("type", "PRE");

                postConditionFolder.put("title", "Postconditions");
                postConditionFolder.put("key", "POST_" + parentId);
                postConditionFolder.put("_id", "POST_" + parentId);
                postConditionFolder.put("folder", true);
                postConditionFolder.put("parentId", parentId);
                postConditionFolder.put("parentName", parentName);
                postConditionFolder.put("executionOrder", 9999.0);
                postConditionFolder.put("hierarchy", hierarchy + 1);
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

            if (Objects.nonNull(document.get(key))) {

                List<Document> childDocs = (List<Document>) document.get(key);
                childDocs.forEach((childDoc) ->
                {
                    childDoc.put("modifiedOn", CommonUtil.getFormattedDate(String.valueOf(childDoc.get("modifiedOn"))));
                    childDoc.put("parentId", document.get("_id"));
                    childDoc.put("title", childDoc.get("name"));

                    keyDocumentList.add(childDoc);
                });
                document.remove(key);

            }

        });

        documentList.addAll(keyDocumentList);

        return documentList;

    }

    private static StringBuilder convertToFancyTree(StringBuilder fancyTreeBuilder, TreeNode<Document> documentParent, String key) {

        int count = documentParent.children.size();
        for (int i = 0; i < count; i++) {

            TreeNode<Document> child = documentParent.children.get(i);
            fancyTreeBuilder.append(child.data.toJson()).append(",");

            if (child.children.size() > 0) {
                fancyTreeBuilder.append("children: [ ");
                convertToFancyTree(fancyTreeBuilder, child, key);
                if (i == count - 1) {
                    fancyTreeBuilder.append("] } ");
                } else {
                    fancyTreeBuilder.append("] }, ");
                }
            }

        }
        return fancyTreeBuilder;
    }

    private static void sortElementDocument(LinkedList<TreeNode<Document>> children) {
        if (isContainsElements(children)) {
            Collections.sort(children, (child1, child2) -> {
                String elementId1 = (String) child1.data.get(CommonConstants.ELEMENT_ID);
                String elementId2 = (String) child2.data.get(CommonConstants.ELEMENT_ID);
                if (StringUtils.isBlank(elementId1)) {
                    return (StringUtils.isBlank(elementId2)) ? 0 : -1;
                }
                if (StringUtils.isBlank(elementId2)) {
                    return 1;
                }
                return elementId1.compareTo(elementId2);
            });
        }
    }

    private static boolean isContainsElements(LinkedList<TreeNode<Document>> children) {
        for (TreeNode<Document> child : children) {
            if(Objects.nonNull(child.data.get(CommonConstants.ELEMENT_ID))) {
                return true;
            }
        }
        return false;
    }

    private static List<Document> reOrderParent(List<Document> documentList, String rootId) {
        List<Document> reOrderedDocumentList = Collections.synchronizedList(new ArrayList<>());
        List<String> selectIdList = Collections.synchronizedList(new ArrayList<>());
        getFutureReOrderParent(documentList, reOrderedDocumentList, selectIdList, rootId);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            log.error("Exception in reOrderParent: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
        }

        Collections.sort(reOrderedDocumentList, new SortByExecutionOrder());

        reOrderedDocumentList.forEach(document -> {
            if (Objects.nonNull(document.get("hierarchy")) && Objects.nonNull(document.get("executionOrder"))) {
                String exec = String.valueOf(document.get("executionOrder"));
                String[] parts = exec.split("\\.");
                exec = StringUtils.leftPad(parts[0], 5, "0");
                String hierarchy = String.valueOf(document.get("hierarchy"));
                String increaseExOrder = hierarchy + exec + "." + parts[1];
                double exOrder = Double.parseDouble(increaseExOrder);
                document.put("executionOrder", exOrder);
            }
        });

        Collections.sort(reOrderedDocumentList, new SortByExecutionOrder());

        reOrderedDocumentList.forEach(document -> {
            if (Objects.nonNull(document.get("hierarchy")) && Objects.nonNull(document.get("executionOrder")) && !document.getInteger("hierarchy").equals(0)) {
                String hierarchy = String.valueOf(document.get("hierarchy"));
                String execOrder = String.valueOf(document.get("executionOrder"));
                int h = hierarchy.length();
                int e = execOrder.length();
                String exOrder = execOrder.substring(h);
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

    @Async
    public static void getFutureReOrderParent(List<Document> documentList, List<Document> reOrderedDocumentList, List<String> selectIdList, String rootId) {
        ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
        int docSize = batchSize;
        for (int start = 0; start <= documentList.size(); start += docSize) {

            int end = Math.min(start + docSize, documentList.size());
            List<Document> subList = documentList.subList(start, end);
            CompletableFuture<List<Document>> fancyTreeBuilderCompletableFuture = CompletableFuture.supplyAsync(() -> reOrderedDocumentList(subList, reOrderedDocumentList, selectIdList, rootId), executor);
            try {
                fancyTreeBuilderCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception in fancyTreeBuilderCompletableFuture: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
            }
        }
        executor.shutdown();
    }

    private static List<Document> reOrderedDocumentList(List<Document> subList, List<Document> reOrderedDocumentList, List<String> selectIdList, String rootId) {
        for (Document documentList : subList) {

            selectIdList.add(String.valueOf(documentList.get("_id")));
            String parentId = String.valueOf(documentList.get("parentId"));

            if ((!selectIdList.contains(parentId))) {
                if (!CommonConstants.ROOT.equalsIgnoreCase(String.valueOf(documentList.get("name")))) {
                    if (StringUtils.isEmpty(rootId) || (Objects.nonNull(rootId) && !rootId.equalsIgnoreCase(String.valueOf(documentList.get("_id"))))) {

                        Optional<Document> parentDocOptional = subList.stream().filter(parentDoc -> parentDoc.getString("_id").
                                equals(documentList.getString("parentId"))).findFirst();
                        if (parentDocOptional.isPresent()) {
                            Document parentDoc = parentDocOptional.get();

                            if (!reOrderedDocumentList.contains(parentDoc)) {
                                reOrderedDocumentList.add(parentDoc);
                            }
                        }
                    }
                }
            }

            if (!reOrderedDocumentList.contains(documentList)) {
                reOrderedDocumentList.add(documentList);
            }
        }
        return reOrderedDocumentList;
    }


    @Async
    public static CompletableFuture<TreeNode<Document>> getFutureConstructTree(List<Document> documentList, String rootId, String testCaseType) {
        TreeNode<Document> root = new TreeNode<>(new Document());
        try {
            Thread.sleep(10);

            ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
            int docSize = batchSize;

            for (int start = 0; start <= documentList.size(); start += docSize) {

                int end = Math.min(start + docSize, documentList.size());
                List<Document> subList = documentList.subList(start, end);

                CompletableFuture<TreeNode<Document>> treeNodeCompletableFuture = CompletableFuture.supplyAsync(() -> constructTree(subList, rootId, testCaseType, root, documentList), executor);
                treeNodeCompletableFuture.get();
            }
            executor.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception in getFutureConstructTree: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));

        }
        return CompletableFuture.completedFuture(root);
    }

    private static TreeNode<Document> constructTree(List<Document> subDocumentList, String rootId, String
            testCaseType, TreeNode<Document> root, List<Document> documentList) {

        Set<String> fancyTreeFolderSet = new HashSet(CommonConstants.fancyTreeFolderList);
        List<String> listOfDuplicates = new ArrayList<>();
        subDocumentList.forEach(document -> {
            String docType = document.get("_id").toString().substring(0, 3);
            if (document.get("_id").equals(rootId) || Objects.isNull(document.get("parentId"))) {
                root.addChild(document);
            } else {
                TreeNode<Document> parent = findDocumentParent(root, document);
                if (Objects.nonNull(parent)) {
                    String key = Objects.nonNull(parent.data.get("key")) ? parent.data.get("key").toString() : null;
                    if (document.get("_id").toString().startsWith("SCR") && !listOfDuplicates.contains(document.get("_id").toString())) {
                        List<Document> listOfDocuments = subDocumentList.stream().filter(o -> o.get("_id").toString().startsWith("SCR") &&
                                o.get("parentId").equals(document.get("parentId")) && o.get("name").equals(document.get("name")) &&
                                o.get("scriptType").equals(document.get("scriptType"))).collect(Collectors.toList());
                        List<Map<String, String>> listOfMap = new ArrayList<>();
                        String testDoc = Objects.nonNull(document.get("testCaseType")) ? document.get("testCaseType").toString() : CommonConstants.automation;
                        boolean flag = false;
                        if (Objects.nonNull(testCaseType) && testCaseType.equalsIgnoreCase(testDoc) && listOfDocuments.size() == 1 || Objects.isNull(testCaseType)) {
                            flag = true;
                        }
                        if (flag) {
                            Map<String, String> typeCheck = new HashMap<>();
                            List<String> getDescAndParentName = new ArrayList<>();
                            listOfDocuments.stream().forEach(o -> {
                                Map<String, String> map = new HashMap<>();
                                String type = Objects.nonNull(o.get("testCaseType")) ? o.get("testCaseType").toString() : CommonConstants.automation;
                                typeCheck.put(type, type);
                                getDescAndParentName.add(Objects.nonNull(o.get("desc")) ? o.get("desc").toString() : null);
                                getDescAndParentName.add(Objects.nonNull(o.get("parentName")) ? o.get("parentName").toString() : null);
                                if (Objects.nonNull(testCaseType) && testCaseType.equalsIgnoreCase(type) || Objects.isNull(testCaseType)) {
                                    map.put("type", type);
                                    map.put("id", o.get("_id").toString());
                                    listOfMap.add(map);
                                    listOfDuplicates.add(o.get("_id").toString());
                                }
                            });
                            document.put("testCaseType", listOfMap);
                            document.put("desc", getDescAndParentName.get(0));
                            document.put("parentName", getDescAndParentName.get(1));
                            if (typeCheck.containsKey(CommonConstants.automation))
                                document.put("containsAutomationScript", true);
                            else
                                document.put("containsAutomationScript", false);
                            parent.addChild(document);
                        }
                    } else if (!document.get("_id").toString().startsWith("SCR"))
                        parent.addChild(document);
                    if (Objects.nonNull(key) && (key.startsWith("PRE_") || key.startsWith("POST_"))) {
                        parent.data.put("scriptCount", parent.children.size());
                    }
                }
            }
            if (fancyTreeFolderSet.contains(docType)) {
                document.put("folder", true);
                ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
                CompletableFuture<Document> fancyTreeBuilderCompletableFuture = CompletableFuture.supplyAsync(() -> updateModuleScriptCounts(documentList, document), executor);
                try {
                    fancyTreeBuilderCompletableFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Exception in fancyTreeBuilderCompletableFuture: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
                }
                executor.shutdown();
            }
        });
        return root;
    }

    @Async
    public static CompletableFuture<TreeNode<Document>> getFutureConstructTreeWithoutResources
            (List<Document> documentList, String rootId) {

        ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
        int docSize = batchSize;
        TreeNode<Document> root = new TreeNode<>(new Document());
        for (int start = 0; start <= documentList.size(); start += docSize) {

            int end = Math.min(start + docSize, documentList.size());
            List<Document> subList = documentList.subList(start, end);

            CompletableFuture<TreeNode<Document>> treeNodeCompletableFuture = CompletableFuture.supplyAsync(() -> constructTreeWithoutResources(subList, rootId, root, documentList), executor);
            try {
                treeNodeCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception in getFutureConstructTreeWithoutResources: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
            }
        }
        executor.shutdown();
        return CompletableFuture.completedFuture(root);
    }

    private static TreeNode<Document> constructTreeWithoutResources(List<Document> subDocumentList, String
            rootId, TreeNode<Document> root, List<Document> documentList) {
        Set<String> fancyTreeFolderSet = new HashSet<>(CommonConstants.fancyTreeFolderList);

        subDocumentList.forEach(document -> {
            String docType = document.get("_id").toString().substring(0, 3);
            if (document.get("_id").equals(rootId) || Objects.isNull(document.get("parentId"))) {
                root.addChild(document);
            } else if (fancyTreeFolderSet.contains(docType)) {
                TreeNode<Document> parent = findDocumentParent(root, document);
                parent.addChild(document);
            }
            if (fancyTreeFolderSet.contains(docType)) {
                document.put("folder", true);
                ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
                CompletableFuture<Document> fancyTreeBuilderCompletableFuture = CompletableFuture.supplyAsync(() -> updateModuleScriptCounts(documentList, document), executor);
                try {
                    fancyTreeBuilderCompletableFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Exception in fancyTreeBuilderCompletableFuture: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
                }
                executor.shutdown();
            }
        });

        return root;
    }

    private static Document updateModuleScriptCounts(List<Document> documentList, Document document) {
        long moduleCount = documentList.stream().filter(doc -> !document.getString("_id").equals(doc.getString("_id")) && Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && Objects.isNull(doc.get("type"))).count();
        document.put("subModuleCount", (int) moduleCount);
        List<Document> scriptList = documentList.stream().filter(doc -> Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && Objects.nonNull(doc.get("type")) && doc.get("type").equals(CommonConstants.script)).collect(Collectors.toList());
        document.put("scriptCount", scriptCountBasedOnCriteria(scriptList));
        List<Document> moduleLevelScriptList = documentList.stream().filter(doc -> Objects.nonNull(doc.get("type")) && doc.get("type").equals(CommonConstants.script) && Objects.nonNull(doc.get("parentId")) && doc.get("parentId").toString().equalsIgnoreCase(document.get("_id").toString())).collect(Collectors.toList());
        document.put("moduleLevelScriptCount", scriptCountBasedOnCriteria(moduleLevelScriptList));
        return document;
    }

    @Async
    public static CompletableFuture<TreeNode<Document>> getFutureConstructFolderSortedTree
            (List<Document> documentList, String rootId, String key, boolean resourceRequired) {

        ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
        TreeNode<Document> root = new TreeNode<>(new Document());
        CompletableFuture<TreeNode<Document>> treeNodeCompletableFuture = CompletableFuture.supplyAsync(() -> constructFolderSortedTree(documentList, rootId, key, root, resourceRequired), executor);
        try {
            treeNodeCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception in fancyTreeBuilderCompletableFuture : " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
        }
        executor.shutdown();
        return CompletableFuture.completedFuture(root);
    }

    private static TreeNode<Document> constructFolderSortedTree(List<Document> documentList, String
            rootId, String key, TreeNode<Document> root, boolean resourceRequired) {
        Set<String> fancyTreeFolderSet = new HashSet(CommonConstants.fancyTreeFolderList);
        Map<String, String> keyMap = getCountKey(key);
        documentList.forEach(document -> {
            if (document.get("_id").equals(rootId) || Objects.isNull(document.get("parentId"))) {
                root.addChild(document);
                String docType = document.get("_id").toString().substring(0, 3);
                if (fancyTreeFolderSet.contains(docType)) {
                    document.put("folder", true);
                    ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
                    CompletableFuture<Document> treeNodeCompletableFuture = CompletableFuture.supplyAsync(() -> updateTreeCount(documentList, document, keyMap), executor);
                    try {
                        treeNodeCompletableFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Exception in fancyTreeBuilderCompletableFuture: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
                    }
                    executor.shutdown();
                }
            } else {
                TreeNode<Document> parent = findDocumentParent(root, document);
                if (Objects.nonNull(parent)) {
                    String docType = document.get("_id").toString().substring(0, 3);
                    if (fancyTreeFolderSet.contains(docType)) {
                        document.put("folder", true);
                        parent.addChild(document);
                        ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
                        CompletableFuture<Document> treeNodeCompletableFuture = CompletableFuture.supplyAsync(() -> updateTreeCount(documentList, document, keyMap), executor);
                        try {
                            treeNodeCompletableFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("Exception in fancyTreeBuilderCompletableFuture: " + e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()));
                        }
                        executor.shutdown();
                    } else if (resourceRequired) {
                        parent.addInnerChild(document);
                    }
                }
            }
        });
        return root;
    }

    private static Document updateTreeCount(List<Document> documentList, Document
            document, Map<String, String> keyMap) {
        long folderCount = documentList.stream().filter(doc -> !document.getString("_id").equals(doc.getString("_id")) && Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString()) && Objects.nonNull(doc.getBoolean("folder")) && doc.getBoolean("folder")).count();
        document.put(keyMap.get("folderKey"), (int) folderCount);
        long childCount = 0;
        if (keyMap.get("childKey").equalsIgnoreCase("elementCount")) {
            childCount = documentList.stream().filter(doc -> (Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString())) && (Objects.nonNull(doc.getBoolean("folder")) && !doc.getBoolean("folder")) && (Objects.isNull(doc.get("isShared")) || !doc.get("isShared").toString().equals("Y"))).count();
        } else if (keyMap.get("childKey").equalsIgnoreCase("stepGroupCount")) {
            childCount = documentList.stream().filter(doc -> (Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString())) && (Objects.nonNull(doc.getBoolean("folder")) && doc.getBoolean("folder"))).mapToInt(o -> (int) o.get("stepGroupCount")).sum();
        } else {
            childCount = documentList.stream().filter(doc -> (Objects.nonNull(doc.get("searchKey")) && doc.get("searchKey").toString().contains(document.get("_id").toString())) && (Objects.nonNull(doc.getBoolean("folder")) && !doc.getBoolean("folder"))).count();
        }
        document.put(keyMap.get("childKey"), (int) childCount);
        return document;
    }

    private static Map<String, String> getCountKey(String key) {
        Map<String, String> keyMap = new HashMap<>();
        switch (key.toLowerCase()) {
            case "elements":
                keyMap.put("folderKey", "subPageCount");
                keyMap.put("childKey", "elementCount");
                break;
            case "scripts":
                keyMap.put("folderKey", "subModuleCount");
                keyMap.put("childKey", "scriptCount");
                break;
            case "programelements":
                keyMap.put("folderKey", "subPackageCount");
                keyMap.put("childKey", "programElementCount");
                break;
            case "stepgroups":
                keyMap.put("folderKey", "subLibraryCount");
                keyMap.put("childKey", "stepGroupCount");
                break;
            case "files":
            default:
                keyMap.put("folderKey", "subFolderCount");
                keyMap.put("childKey", "fileCount");
                break;
        }
        return keyMap;
    }

    private static TreeNode<Document> findDocumentParent(TreeNode<Document> treeRoot, Document document) {

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

    @Async
    public static CompletableFuture<List<CompletableFuture<List<Document>>>> divide
            (List<Document> arrayList, String scripts) {

        ExecutorService executor = Executors.newFixedThreadPool(fixedThreadPoolSize);
        int docSize = batchSize;
        List<CompletableFuture<List<Document>>> allresults = new ArrayList<>();
        for (int start = 0; start <= arrayList.size(); start += docSize) {
            int end = Math.min(start + docSize, arrayList.size());
            List<Document> subList = arrayList.subList(start, end);
            CompletableFuture<List<Document>> result = CompletableFuture.supplyAsync(() -> flattenTree(subList, scripts), executor);
            allresults.add(result);
        }
        executor.shutdown();
        return CompletableFuture.completedFuture(allresults);
    }

    private static int scriptCountBasedOnCriteria(List<Document> documentList) {

        AtomicInteger count = new AtomicInteger(0);
        Set<String> scriptList = new HashSet<>();
        documentList.stream().forEach((document) -> {
            String parentIdNameColonType = document.getString("parentId") + ":" + document.getString("name") + ":" + document.getString("scriptType");
            if (!scriptList.contains(parentIdNameColonType)) {
                scriptList.add(parentIdNameColonType);
                count.getAndIncrement();
            }
        });
        return count.get();
    }

}
