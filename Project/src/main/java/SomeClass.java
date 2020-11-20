import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.util.ArrayList;

class SomeClass {
    AnalysisScope scope;

    ArrayList<String> loadClass(String target_classs, String target_test_class) throws Exception{
        ArrayList<String> testClassName=new ArrayList<String>();
        ClassLoader classloader = SomeClass.class.getClassLoader();
        scope = AnalysisScopeReader.readJavaScope(
                "scope.txt",
                new File("exclusion.txt"),
                classloader
        );
        File file1 = new File(target_classs);
        File file2 = new File(target_test_class);

        File[] fileList1 = file1.listFiles();
        File[] fileList2 = file2.listFiles();

        if (fileList1 != null) {
            for (File file:fileList1) {
                if (file.isFile()) {
                    String fileName = target_classs+file.getName();
                    //System.out.println(fileName);
                    scope.addClassFileToScope(ClassLoaderReference.Application, new File(fileName));
                }
            }
        }
        if (fileList2 != null) {
            for (File file:fileList2) {
                if (file.isFile()) {
                    String fileName = target_test_class+file.getName();
                    testClassName.add(file.getName().replace(".class",""));
                    scope.addClassFileToScope(ClassLoaderReference.Application, new File(fileName));
                }
            }
        }

        return testClassName;
    }
}
