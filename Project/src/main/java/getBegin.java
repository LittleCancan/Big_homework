import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;


public class getBegin {

    private static ArrayList<Node> classNodes=new ArrayList<Node>();//存储所有的类级Node及其调用者
    private static ArrayList<Node> methodNodes=new ArrayList<Node>();//存储所有的方法级Node及其调用者
    private static ArrayList<String> classNameList=new ArrayList<String>();//当前已在classNodes中的类的名称，方便判断某个新的类在classNodes中的位置
    private static ArrayList<Integer> methodIndexList=new ArrayList<Integer>();//存储methodNodes中method对应class在classNodes中的位置

    private static ArrayList<String> change_info=new ArrayList<String>();//读入的change_info
    private static ArrayList<String> testClassName=new ArrayList<String>();//存储所有测试class的文件名

    private static ArrayList<String> selection_class=new ArrayList<String>();//存储selection_class最后的结果
    private static ArrayList<String> selection_method=new ArrayList<String>();//存储selection_method最后的结果


    public static void main(String[] args) throws Exception{
        System.out.println("running...\n");

        String command=args[0];//-x参数
        String project_target=args[1];//<project_target>
        String change_info=args[2];//<change_info>

        String target_classes=project_target+"\\classes\\net\\mooctest\\";
        String target_test_class=project_target+"\\test-classes\\net\\mooctest\\";

        /*加载class文件*/
        SomeClass someClass=new SomeClass();
        testClassName=someClass.loadClass(target_classes,target_test_class);

        AnalysisScope scope=someClass.scope;

        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);

        CHACallGraph cg = new CHACallGraph(cha);
        cg.init(eps);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {

                    // 获取类名和方法签名
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();

                    //存入classNodes,methodNodes中
                    if(!classNameList.contains(classInnerName)){//判断该类是否已经存在在classNodes中
                        Node classNode=new Node();
                        classNode.name=classInnerName;
                        classNameList.add(classNode.name);
                        classNodes.add(classNode);
                    }
                    Node methodNode=new Node();
                    methodNode.name=signature;
                    methodIndexList.add(classNameList.indexOf(classInnerName));
                    methodNodes.add(methodNode);

                    Iterator<CGNode> it=cg.getPredNodes(node);//获得前驱节点
                    addNodes(it,classNameList.indexOf(classInnerName),methodNodes.size()-1);//构建调用list
                }
            }
        }

        read_change_info(change_info);

        if(command.equals("-c")){
            getSelectionClass();
        }else if(command.equals("-m")){
            getSelectionMethod();
        }else{
            System.out.println("命令参数错误");
        }

        System.out.println("End Successfully!\n");
    }

    /*构建调用列表*/
    private static void addNodes(Iterator<CGNode> it, int classIndex, int methodIndex){
        while(it.hasNext()){
            CGNode node=it.next();
            if(node.getMethod() instanceof ShrikeBTMethod){
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();

                    if(!classNodes.get(classIndex).isCalledBy.contains(classInnerName)){
                        classNodes.get(classIndex).isCalledBy.add(classInnerName);
                    }
                    methodNodes.get(methodIndex).isCalledBy.add(signature);
                }
            }
        }
    }


    /*读取change_info文件*/
    private static void read_change_info(String fileName){
        File file=new File(fileName);
        try{
            InputStreamReader streamread = new InputStreamReader(new FileInputStream(file),"GBK");
            BufferedReader read=new BufferedReader(streamread);
            String line;
            while((line = read.readLine()) != null){
                change_info.add(line);
            }
            read.close();
        }catch (Exception e){
            System.out.println("读取文件失败！");
        }
    }

    /*执行类级别测试用例选择操作*/
    private static void getSelectionClass()throws Exception{
        File file=creatFile("./selection_class.txt");
        FileWriter fw = new FileWriter(file, true);
        BufferedWriter bw = new BufferedWriter(fw);

        for(String change:change_info){
            String className= change.split(" ")[0];
            for(Node node: classNodes){
                if(node.name.equals(className)){
                    getCallerClassName(node);
                }
            }
        }
        //格式化类名（再末尾添加被选中的类的方法名），然后写入txt文件
        for(int i=0;i<selection_class.size();i++){
            String name=selection_class.get(0);
            selection_class.remove(name);
            int index=classNameList.indexOf(name);
            for(int m=0;m<methodIndexList.size();m++){
                if(methodIndexList.get(m)==index){
                    if(!methodNodes.get(m).name.contains("<init>") && !methodNodes.get(m).name.contains("initialize")){
                        selection_class.add(name+" "+methodNodes.get(m).name);
                        bw.write(name+" "+methodNodes.get(m).name+'\n');
                    }
                }
            }
        }
        bw.flush();
        bw.close();
        fw.close();
    }

    /*迭代获得当前节点的调用节点Caller，添加至selection_class中*/
    private static void getCallerClassName(Node node){
        for(String name:node.isCalledBy){
            if(isTestClass(name)){
                if(!selection_class.contains(name)){
                    selection_class.add(name);
                }
            }else if(!node.name.equals(name)){
                if(!classNodes.get(classNameList.indexOf(name)).isCalledBy.contains(node.name))
                    getCallerClassName(classNodes.get(classNameList.indexOf(name)));
            }
        }
    }

    /*判断该类是否是测试类*/
    private static boolean isTestClass(String name){
        for(String testName:testClassName){
            if(name.contains(testName)){
                return true;
            }
        }
        return false;
    }


    /*执行方法级测试用例选择的具体操作*/
    private static void getSelectionMethod()throws Exception{
        File file=creatFile("./selection_method.txt");
        FileWriter fw = new FileWriter(file, true);
        BufferedWriter bw = new BufferedWriter(fw);

        for(String change:change_info){
            String methodName= change.split(" ")[1];
            for(Node node: methodNodes){
                if(node.name.equals(methodName)){
                    getCallerMethodName(node);
                }
            }
        }
        //格式化方法名（在方法名前加上对应类名），重新写入selection_method中
        int classIndex;
        for(String name:selection_method){
            for(Node node:methodNodes){
                if(node.name.equals(name) && !name.contains("<init>")){
                    classIndex=methodIndexList.get(methodNodes.indexOf(node));
                    name=classNameList.get(classIndex)+" "+node.name;
                    bw.write(name+'\n');
                    break;
                }
            }
        }
        bw.flush();
        bw.close();
        fw.close();
    }


    /*迭代获得当前节点的调用者Caller，将其加入selection_method*/
    private static void getCallerMethodName(Node node){
        for(String name:node.isCalledBy){
            if(isTestMethod(name)){
                if(!selection_method.contains(name)){
                    selection_method.add(name);
                }
            }else if(!node.name.equals(name)){
                for(Node mNode:methodNodes){
                    if (mNode.name.equals(name)){
                        getCallerMethodName(mNode);
                        break;
                    }
                }
            }
        }
    }


    /*判断当前方法是否是测试方法*/
    private static boolean isTestMethod(String name){
        int methodIndex=0;
        for(Node node:methodNodes){
            if(node.name.equals(name)){
                methodIndex=methodNodes.indexOf(node);
                break;
            }
        }
        return isTestClass(classNodes.get(methodIndexList.get(methodIndex)).name);
    }


    /*创建一个文件*/
    private static File creatFile(String path) throws Exception{
        File file = new File(path);
        if(!file.exists()){
            file.createNewFile();
        }
        return file;
    }
}
