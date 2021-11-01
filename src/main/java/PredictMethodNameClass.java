import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;


import com.intellij.openapi.editor.colors.EditorColors;

import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang.StringUtils;

import org.jetbrains.annotations.NotNull;

import detectId.DS.ClassDS;
import detectId.DS.IdentifierDS;
import detectId.DS.MethodDS;
import detectId.ParseInfo.ClassCollector;
import detectId.ParseInfo.VariableCollector;

import detectId.Trace.SyncPipe;

import detectId.utility.SimilarityCal;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PredictMethodNameClass extends AnAction {
    static List<String> fieldsName;
    static List<String> methodName;
    static List<String> variableName;
    static List<String> callSet;
    //获取declaration
    static Map<String, FieldDeclaration> fieldMap;
    static Map<String, MethodDeclaration> methodMap;
    static Map<String, VariableDeclarationExpr> variableMap;
    static Map<String, String> callMap;
    static Map<String, String> insideMethodMap;
    static List<String> results;

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getData(PlatformDataKeys.PROJECT);


        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        String filePath = psiFile.getVirtualFile().getPath();

        String proj =psiFile.getVirtualFile().getParent().getPath();


        System.out.println("file="+filePath);
        Map<String, List> map=new HashMap<>();

        File f=new File(filePath);

        if(!f.exists()) {

        }
        try {
            map = JavaParserUtils.getData(filePath);

        } catch (Exception exception) {


        }
        fieldsName = map.get("fields_name");
        methodName = map.get("method_name");
        variableName = map.get("variable_name");
        callSet = map.get("call_relation");
        fieldMap = JavaParserUtils.fieldMap;
        methodMap = JavaParserUtils.methodMap;
        variableMap = JavaParserUtils.variableMap;
        callMap = JavaParserUtils.nameExprMap;
        insideMethodMap=JavaParserUtils.InsideMethodMap;
        System.out.println("[method Name]="+methodName.toString());
        //遍历所有方法名字

        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        removehighlightMatch(editor);
        for(String method:methodName){
            String cmd="git log -L:"+method+":"+filePath;

            String output=proj+"/output.txt";
            cmd("c:",proj,cmd,output);
            StringBuffer model_res=new StringBuffer();
            try {
                Vector<commitMessage> allmessage = new Vector<>();
//                System.out.println(allmessage.size());
                allmessage = ParseCommandContent(output);
//                System.out.println(allmessage.size());
                StringBuffer histStmt = new StringBuffer();
                StringBuffer curStmt = new StringBuffer();

                commitMessage cm = allmessage.get(0);

                String[] hist = cm.getHistoricalStmt().split("\n");
                String[] cur = cm.getCurStmt().split("\n");

                for (String one_hist : hist) {
                    one_hist = one_hist.trim();
                    if (one_hist.startsWith("-")) {
                        histStmt.append(one_hist.substring(1).trim());

                    } else {
                        histStmt.append(one_hist.trim());

                    }
                    if (one_hist.startsWith("//")) continue;
                }
                for (String one_cur : cur) {
                    one_cur = one_cur.trim();
                    if (one_cur.startsWith("+")) {
                        curStmt.append(one_cur.substring(1).trim());

                    } else {
                        curStmt.append(one_cur.trim());

                    }
                    if (one_cur.startsWith("//")) continue;
                }
                //去掉注释
                System.out.println("Historical statement=" + histStmt.toString());
                System.out.println("Current statement=" + curStmt.toString());
                //把选取得到的文本输入到数据预处理器
            String urlString = "http://106.14.236.108:8000/?wsdl";//wsdl文档的地址
            URL url = new URL(urlString);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();//打开连接

//
            String params="<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "<soap:Body>\n" +
                    "<m:say_hello xmlns:m=\"spyne.examples.hello\">\n" +
                    "<m:name>"+histStmt.toString()+"@@@@@"+curStmt.toString()+"</m:name>\n" +
                    "</m:say_hello>\n" +
                    "</soap:Body>\n" +
                    "</soap:Envelope>";
//            System.out.println(params);

            httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);

            OutputStream out = httpConn.getOutputStream();


            out.write(params.getBytes("UTF-8"));
            out.close();


            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                System.out.println("Success!");
                InputStreamReader is = new InputStreamReader(httpConn.getInputStream());
                BufferedReader in = new BufferedReader(is);
                String inputLine;


                while ((inputLine = in.readLine()) != null)
                {


                    inputLine=inputLine.replace("\\n","");
                    inputLine=inputLine.replaceAll("\"","'");
//                    System.out.println(inputLine);
                    String rgex = "<tns:say_helloResult>(.*?)</tns:say_helloResult>";
                    model_res.append(getSubUtilSimple(inputLine, rgex));

//
                }
                in.close();

            }else{
                System.out.println("Fail!");
            }
            httpConn.disconnect();

            }catch (Exception exception){

            }
            System.out.println("[model output]="+model_res);
            //1.根据文件+选中文本 查找所有相关标识符，集合

            ArrayList<String> res=searchRes(method);
            System.out.println("[related entity]="+res.toString());
            //2.由git log统计相关标识符中重命名的次数（这里不实现需要问程序员的）

        //-----------------------------获取重命名方法与HistoryAnalysis一致--》info--》判断oldname=newname?
        //获取该文件中的所有iden，再筛选出其中的一部分

        //allcode:源代码
        Vector<String> allcode = new Vector<String>();
        try {

            BufferedReader read = new BufferedReader(new FileReader(filePath));
            String one = "";
            while ((one = read.readLine()) != null) {
                allcode.add(one);
            }
            read.close();
            //获取所有标识符
            Vector<idenDS> alliden = ObtainIdentifier(allcode, filePath);
            //筛选related iden
            Vector<idenDS> residen = new Vector<>();
            //git log输出文件（分析位置）
            String LogOutput=proj+"/logout.txt";

            for(idenDS s:alliden){
                if(res.contains(s.getIdentifier())) residen.add(s);
            }
            int res_rename_num=0;
            for(idenDS id:residen){
                //执行git
                int lineno = id.getLocation();
                lineno++;
                ExecuteCommand(proj, "git log -L " + lineno + "," + lineno + ":" + filePath, LogOutput);
                //分析git输出和内容
                System.out.println("-----------------------------------------------------------------------------------------------");
                //log输出
                Vector<commitMessage> allcom = ParseCommandContent(LogOutput);

                String statement=id.getStatement();
                Vector<String>[] data=TraceAnalysis(allcom,statement,id);
                Vector<String> traceResult=data[0];

                //有修改历史的话
                if(traceResult.size()>1) {
                    //获取beforeId
                    Vector<String> beforeId = ExtractIdFromStatement(traceResult, id.getIdentifier(), id.getType());
                    System.out.println(beforeId.size());
                    for (String s : beforeId) {
                        System.out.println("[before res identifier]="+s );
                    }
                    System.out.println("[current red identifier]="+id.getIdentifier());

                    //3.对比 before id和current id，计算出相关实体的重命名次数
                    if(!beforeId.get(0).trim().equals(id.getIdentifier())) res_rename_num++;

                }



            }
            //获取当前id
            System.out.println("[current identifier]="+method+"[res renaming num]="+res_rename_num);
            System.out.println("[Model Ouutput]="+model_res.toString());

            //5.细化预测结果
            int s_idx=model_res.indexOf("[[");
            int e_idx=model_res.indexOf("]");
            float p_neg=Float.valueOf(model_res.substring(s_idx+2,e_idx).split(",")[0]);
            float p_pos=Float.valueOf(model_res.substring(s_idx+2,e_idx).split(",")[1]);
            int predict_label=0;
            if(p_pos>p_neg && res_rename_num>0) predict_label=1;
            System.out.println("[Predict Ouutput]="+predict_label);


            if(predict_label==1) {


                for (int j = 0; j< editor.getDocument().getLineCount();j++) {
                    //获取行中的对象


                    int start = editor.getDocument().getLineStartOffset(j);
                    int end = editor.getDocument().getLineEndOffset(j);

                    int idx0=editor.getDocument().getText(new TextRange(start,end)).indexOf(method+" ");
                    int idx1=editor.getDocument().getText(new TextRange(start,end)).indexOf(method+"(");
//                    System.out.println("[idx=]"+idx+method);
                    if(idx0!=-1 ){

                        highlightMatch(editor, start+idx0,start+idx0+method.length() );
                    }else if(idx1!=-1){
                        highlightMatch(editor, start+idx1,start+idx1+method.length() );

                    }


//                    System.out.println("[line]=" + text);
                }





            }

            System.out.println("-------------------------------------------------------------------------------");


        }catch(Exception exception){

        }

        }
//        Project project=e.getProject();
        NotificationGroup notify = new NotificationGroup("com.yatoufang.notify", NotificationDisplayType.BALLOON, true);
        notify.createNotification("方法名检测完成，双击选中已标记为需校正的方法名确认继续进行重构。", NotificationType.INFORMATION).notify(project);

    }
    public static RangeHighlighter highlightMatch(@NotNull Editor editor, int start, int end) {
        TextAttributes color = editor.getColorsScheme().getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
        return editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                color, HighlighterTargetArea.EXACT_RANGE);
    }
    public static void removehighlightMatch(@NotNull Editor editor) {
        editor.getMarkupModel().removeAllHighlighters();
    }

    public static Vector<String>[] TraceAnalysis(Vector<commitMessage> allcom, String statement,idenDS iden) throws Exception
    {
        //传入代码行
        statement=statement.trim();
        Vector<String> traceHistory=new Vector<String>();
        //+++++++++++
        Vector<String> historyId=new Vector<String>();
        //+++++++
        Vector<String> locHis=new Vector<String>();

        traceHistory.add(statement);

        //----------------找与这个statement对应的代码行-------------------

        for(commitMessage m:allcom)
        {

            //获取commit中diff的内容
            Vector<Diff> difflist=m.getDifflist();
            for(Diff onediff:difflist)
            {
                //主要获取内容，其他author啥的就不管了
                Vector<String> content=onediff.getContent();
                //System.out.println("content="+content);


                for(String line:content)
                {
                    //以-开始，就是原文件中被删除的/被修改的一行
                    if(line.startsWith("- "))
                    {
                        //获取纯文本内容
                        line=line.substring(1,line.length()).trim();
                        //计算原来代码 line,和现在的代码行statement
                        //具有高相似度则是同一行代码，并且被修改



                        float simi = SimilarityCal.calEditSimi(line, statement);

                        //System.out.println("========================sim="+simi);
                        //确定statemnet变化了但相似
                        if(simi>0.8&&simi!=1)
                        {
                            //取出历史版本stmt line和当前版本stmt statement中的标识符（就第一个）
                            Vector<String> stmtId=ExtractOneIdFromStatement(statement,iden.getIdentifier(),iden.getType());
                            Vector<String> lineId=ExtractOneIdFromStatement(line,iden.getIdentifier(),iden.getType());
                            System.out.println(statement);
                            for(String s:stmtId){
                                System.out.println(s);
                            }

                            for(String s:lineId){
                                System.out.println(s);
                            }
                            float id_simi=0;
                            if(stmtId.size()!=0 && lineId.size()!=0) {
                                id_simi = SimilarityCal.calEditSimi(stmtId.get(0), lineId.get(0));
                                System.out.println("======id_simi"+id_simi);

                            }
                            //确定标识符的确变化了
                            if(id_simi!=1.0) {

                                statement = line;
                                //加入修改历史中。接着顺延
                                traceHistory.add(statement);
                                historyId.add(m.getCommitid());
                                locHis.add(onediff.getToFile()+"<-"+onediff.getFromFile());

                                //获取commitId

                            }
                            break;
                        }
                    }
                }
            }
        }


        return new Vector[]{traceHistory,historyId,locHis};
    }
    public static Vector<String> ExtractIdFromStatement(Vector<String> traceResult, String identifier, int type) throws Exception
    {
        Vector<String> result=new Vector<String>();
        for(int i=1;i<traceResult.size();i++)
//		for(String onetrace:traceResult)
        {
            String onetrace=traceResult.get(i);
            onetrace=onetrace.trim();
            if(onetrace.endsWith(";"))
                onetrace=onetrace.substring(0,onetrace.length()-1);
            onetrace=onetrace.trim();

            if(type==1)
            {
                if(onetrace.startsWith("/*"))
                    onetrace=onetrace.substring(onetrace.indexOf("*/")+2, onetrace.length()).trim();
                if(onetrace.startsWith("//"))
                    onetrace=onetrace.substring(onetrace.indexOf("package "),onetrace.length());
                if(onetrace.startsWith("package "))
                    onetrace=onetrace.substring(onetrace.indexOf(" ")+1,onetrace.length());

                result.add(onetrace);
            }
            else if(type==2)
            {
                if(onetrace.endsWith("{"))
                    onetrace=onetrace.substring(0, onetrace.length()-1);
                if(onetrace.contains(" implements "))
                    onetrace=onetrace.substring(0, onetrace.indexOf(" implements ")).trim();

                if(onetrace.contains(" extends "))
                    onetrace=onetrace.substring(0, onetrace.indexOf(" extends ")).trim();

                if(onetrace.contains(" "))
                {
                    onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                    result.add(onetrace);
                }
                else
                {
                    result.add(onetrace);
                }

            }
            else if(type==3)
            {
                if(onetrace.contains("("))
                    onetrace=onetrace.substring(0, onetrace.indexOf("(")).trim();

                if(onetrace.contains(" "))
                {
                    onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                    result.add(onetrace);
                }
                else
                {
                    result.add(onetrace);
                }

            }
            else if(type==4)
            {
                if(onetrace.contains("="))
                    onetrace=onetrace.substring(0, onetrace.indexOf("=")).trim();
                if(onetrace.contains(" "))
                {
                    onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                    result.add(onetrace);
                }
                else
                {
                    result.add(onetrace);
                }

            }
            else if(type==5)
            {
                if(!onetrace.contains("=")&&onetrace.contains("("))
                {
                    if(onetrace.contains(")"))
                    {
                        onetrace=onetrace.substring(onetrace.indexOf("(")+1, onetrace.indexOf(")"));
                    }
                    else
                    {
                        onetrace=onetrace.substring(onetrace.indexOf("(")+1, onetrace.length()).trim();
                    }

                    if(onetrace.contains(","))
                    {
                        Vector<String> needtest=new Vector<String>();
                        String spl[]=onetrace.split(",");
                        for(String onespl:spl)
                        {
                            onespl=onespl.trim();
                            String test=onespl.trim();
                            if(onespl.contains(" "))
                            {
                                test=onespl.substring(onespl.lastIndexOf(" ")+1, onespl.length());
                            }
                            needtest.add(test);

                        }

                        float max=0;
                        String maxstring="";
                        for(String ss:needtest)
                        {
                            float simi= SimilarityCal.calEditSimi(ss, identifier);
                            if(simi>max)
                            {
                                max=simi;
                                maxstring=ss;
                            }
                        }
                        result.add(maxstring);

                    }
                    else if(onetrace.contains(":"))
                    {
                        onetrace=onetrace.substring(0,onetrace.indexOf(":")).trim();
                        String test=onetrace;
                        if(onetrace.contains(" "))
                        {
                            test=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length());
                        }
                        result.add(test);
                    }
                    else if(onetrace.contains(";"))
                    {
                        onetrace=onetrace.substring(0,onetrace.indexOf(";")).trim();
                        String test=onetrace;
                        if(onetrace.contains(" "))
                        {
                            test=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length());
                        }
                        result.add(test);
                    }
                    else
                    {
                        String test=onetrace.trim();
                        if(onetrace.contains(" "))
                        {
                            test=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length());
                        }
                        result.add(test);
                    }


                }
                else
                {
                    if(onetrace.contains("="))
                        onetrace=onetrace.substring(0, onetrace.indexOf("=")).trim();

                    if(onetrace.contains(" "))
                    {
                        onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                        result.add(onetrace);
                    }
                    else
                    {
                        result.add(onetrace);
                    }
                }
            }

        }
        return result;
    }
    private static Vector<String> ExtractOneIdFromStatement(String statement, String identifier, int type) throws Exception {
        Vector<String> result=new Vector<String>();
//        for(int i=1;i<traceResult.size();i++)
////		for(String onetrace:traceResult)
//        {

        String onetrace=statement;

        onetrace=onetrace.trim();
        if(onetrace.endsWith(";"))
            onetrace=onetrace.substring(0,onetrace.length()-1);
        onetrace=onetrace.trim();

        if(type==1)
        {
            if(onetrace.startsWith("/*"))
                onetrace=onetrace.substring(onetrace.indexOf("*/")+2, onetrace.length()).trim();
            if(onetrace.startsWith("//"))
                onetrace=onetrace.substring(onetrace.indexOf("package "),onetrace.length());
            if(onetrace.startsWith("package "))
                onetrace=onetrace.substring(onetrace.indexOf(" ")+1,onetrace.length());

            result.add(onetrace);
        }
        else if(type==2)
        {
            if(onetrace.endsWith("{"))
                onetrace=onetrace.substring(0, onetrace.length()-1);
            if(onetrace.contains(" implements "))
                onetrace=onetrace.substring(0, onetrace.indexOf(" implements ")).trim();

            if(onetrace.contains(" extends "))
                onetrace=onetrace.substring(0, onetrace.indexOf(" extends ")).trim();

            if(onetrace.contains(" "))
            {
                onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                result.add(onetrace);
            }
            else
            {
                result.add(onetrace);
            }

        }
        else if(type==3)
        {
            if(onetrace.contains("("))
                onetrace=onetrace.substring(0, onetrace.indexOf("(")).trim();

            if(onetrace.contains(" "))
            {
                onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                result.add(onetrace);
            }
            else
            {
                result.add(onetrace);
            }

        }
        else if(type==4)
        {
            if(onetrace.contains("="))
                onetrace=onetrace.substring(0, onetrace.indexOf("=")).trim();
            if(onetrace.contains(" "))
            {
                onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                result.add(onetrace);
            }
            else
            {
                result.add(onetrace);
            }

        }
        else if(type==5)
        {
            if(!onetrace.contains("=")&&onetrace.contains("("))
            {
                if(onetrace.contains(")"))
                {
                    onetrace=onetrace.substring(onetrace.indexOf("(")+1, onetrace.indexOf(")"));
                }
                else
                {
                    onetrace=onetrace.substring(onetrace.indexOf("(")+1, onetrace.length()).trim();
                }

                if(onetrace.contains(","))
                {
                    Vector<String> needtest=new Vector<String>();
                    String spl[]=onetrace.split(",");
                    for(String onespl:spl)
                    {
                        onespl=onespl.trim();
                        String test=onespl.trim();
                        if(onespl.contains(" "))
                        {
                            test=onespl.substring(onespl.lastIndexOf(" ")+1, onespl.length());
                        }
                        needtest.add(test);

                    }

                    float max=0;
                    String maxstring="";
                    for(String ss:needtest)
                    {
                        float simi= SimilarityCal.calEditSimi(ss, identifier);
                        if(simi>max)
                        {
                            max=simi;
                            maxstring=ss;
                        }
                    }
                    result.add(maxstring);

                }
                else if(onetrace.contains(":"))
                {
                    onetrace=onetrace.substring(0,onetrace.indexOf(":")).trim();
                    String test=onetrace;
                    if(onetrace.contains(" "))
                    {
                        test=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length());
                    }
                    result.add(test);
                }
                else if(onetrace.contains(";"))
                {
                    onetrace=onetrace.substring(0,onetrace.indexOf(";")).trim();
                    String test=onetrace;
                    if(onetrace.contains(" "))
                    {
                        test=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length());
                    }
                    result.add(test);
                }
                else
                {
                    String test=onetrace.trim();
                    if(onetrace.contains(" "))
                    {
                        test=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length());
                    }
                    result.add(test);
                }


            }
            else
            {
                if(onetrace.contains("="))
                    onetrace=onetrace.substring(0, onetrace.indexOf("=")).trim();

                if(onetrace.contains(" "))
                {
                    onetrace=onetrace.substring(onetrace.lastIndexOf(" ")+1, onetrace.length()).trim();
                    result.add(onetrace);
                }
                else
                {
                    result.add(onetrace);
                }
            }
        }

//        }
        return result;
    }

    private String getSubUtilSimple(String soap, String rgex) {
        Pattern pattern = Pattern.compile(rgex);
        Matcher m = pattern.matcher(soap);
        while(m.find()){
            return m.group(1);
        }
        return "";
    }

    public static void cmd(String loc,String proj,String cmd,String output){

        String[] command =
                {
                        "cmd",
                };
        try {
            Process p = Runtime.getRuntime().exec(command);

            PrintWriter stdin = new PrintWriter(p.getOutputStream());
            stdin.println(loc);
            stdin.println("cd " + proj);


            System.out.println(cmd);

            stdin.println(cmd.trim()+">"+output);
            stdin.close();
            p.waitFor();

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void readTxtFile(String filePath){
        try {
            String encoding="UTF-8";
            File file=new File(filePath);
            if(file.isFile() && file.exists()){
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file),encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while((lineTxt = bufferedReader.readLine()) != null){
                    System.out.println(lineTxt);
                }
                read.close();
            }else{
                System.out.println("找不到指定的文件");
            }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
        }

    }
    public static Vector<commitMessage> ParseCommandContent(String filename) throws Exception
    {
        //allmessage:
        //allc:输出信息的每一行
        Vector<commitMessage> allmessage=new Vector<commitMessage>();
        Vector<String> allc=new Vector<String>();
        //git log的输出所有信息在filename中，读取该文件
        BufferedReader br=new BufferedReader(new FileReader(filename));
        String lines="";
        while((lines=br.readLine())!=null)
        {

            allc.add(lines);
        }
        br.close();


        //commitinfo:
        Vector<Vector<String>> commitinfo=new Vector<Vector<String>>();
        //一条commit msg
        Vector<String> onecom=new Vector<String>();
        //开始遍历输出信息进行解析,只需要最后一个commit

        for(int i=0;i<allc.size();i++)
        {

            /*结构:
            commit Id
            Author:
            Date:
            xxx
            diff --git a/srcFile b/dstFile
            */
            //新的一条commitMsg要存到oneCom中，如果上一次的还没处理，就存到commitInfo之后再存入
            if(allc.get(i).startsWith("commit "))
            {


                //onecom是第一条的话，直接把所有一句句加入到onecom上，否则先清理（这里其实不需要了）
                if(onecom.size()!=0)
                {

                    Vector<String> temp=new Vector<String>();
                    temp.addAll(onecom);
                    commitinfo.add(temp);
                    onecom.clear();
                    onecom.add(allc.get(i));
                }
                else
                {
                    onecom.add(allc.get(i));
                }

            }
            else if(allc.get(i).equals("\\ No newline at end of file")){
                commitinfo.add(onecom);
                break;
            }
            else
            {
                onecom.add(allc.get(i));
            }

        }

//        if(onecom.size()!=0)
//        {
//            Vector<String> temp=new Vector<String>();
//            temp.addAll(onecom);
//            commitinfo.add(temp);
//            onecom.clear();
//        }


//		System.out.println("commit NO: "+commitinfo.size());

        //把命令内容已经全部存好，对应的每一条commit msg如下

        for(Vector<String> onecommit:commitinfo)
        {
//			for(String ssss:onecommit)
//				System.out.println(ssss);
            String commitid="";
            String author="";
            String date="";
            String message="";
            StringBuilder sb=new StringBuilder();
            StringBuilder historicalStmt=new StringBuilder();
            StringBuilder curStmt=new StringBuilder();
            boolean isDiff=false;

            for(String line:onecommit)
            {
//                System.out.println(line);

                if(line.startsWith("commit"))
                {
                    commitid=line.substring("commit".length(), line.length()).trim();
                }
                else if(line.startsWith("Author:"))
                {
                    author=line.substring("Author:".length(), line.length()).trim();
                }
                else if(line.startsWith("Date:"))
                {
                    date=line.substring("Date:".length(), line.length()).trim();
                }
                else if(line.startsWith("    "))
                {
                    message+=line.trim()+"\n";
                }
                else
                {
                    sb.append(line+"\n");
                }
                //
                if(line.startsWith("diff --git")) isDiff=true;
                if(line.startsWith("    ")&& isDiff){
                    historicalStmt.append(line+"\n");
                    curStmt.append(line+"\n");

                }else if(line.startsWith("+ ")){
                    curStmt.append(line+"\n");
                }else if(line.startsWith("- ")){
                    historicalStmt.append(line+"\n");
                }


            }
            Vector<Diff> difflist=new Vector<Diff>();
            String mess=sb.toString();
            //split数组中每个元素是一个commit的
            String split[]=mess.split("diff --git");

//			System.out.println(split.length-1);
            for(String s:split)
            {
                if(!s.trim().isEmpty())
                {
                    Diff onediff=AnalyzeDiff("diff --git "+s.trim());
//					System.out.println(onediff.toString());
//					System.out.println("****************************");
                    difflist.add(onediff);
                }
            }


            commitMessage onecommitmessage=new commitMessage(commitid, author, date, message,difflist,historicalStmt.toString(),curStmt.toString());
            allmessage.add(onecommitmessage);
        }
        return allmessage;
    }
    public static Diff AnalyzeDiff(String s)
    {
        String split[]=s.split("\n");
        String fromFile="";
        String toFile="";
        String index="";
        Vector<String> content=new Vector<String>();
        for(String oneline:split)
        {
            oneline=oneline.trim();
            if(oneline.startsWith("--- "))
            {
                fromFile=oneline.substring(oneline.indexOf("--- ")+"--- ".length(), oneline.length());
            }
            else if(oneline.startsWith("+++ "))
            {
                toFile=oneline.substring(oneline.indexOf("+++ ")+"+++ ".length(), oneline.length());
            }
            else if(oneline.startsWith("+++ "))
            {
                toFile=oneline.substring(oneline.indexOf("+++ ")+"+++ ".length(), oneline.length());
            }
            else if(oneline.startsWith("@@"))
            {
                index=oneline.replace("@", "").trim();
            }
            else
            {
                if(!oneline.startsWith("diff --git ")&&!oneline.isEmpty())
                    content.add(oneline);
            }
        }

        Diff one=new Diff(fromFile,toFile,index,content);
        return one;
    }
    private static ArrayList<String> searchRes(String ent) {
// 1）Inclusion：包含e直接包含的实体和直接包含e的元素。
//2）Sibling：e是一个方法，同一个类中的所有方法和字段都被认为是紧密相关的实体
//3）Reference：e所引用的所有实体和引用e的实体
//4)   Inheritance：e是一个类，则其超类和子类




        //加入相关的

        Set<String> set = new HashSet<>();
//        set.add(ent);
        //1.Inclusion-method：实体是函数，包含该函数的实体，这个包含只有除了函数以外的实体包含
        if(methodName.contains(ent)){

            MethodDeclaration m=methodMap.get(ent);
            //包含这个函数的肯定是类或其他函数等等
            String[] classandother=JavaParserUtils.getParents(m).split("_");

            set.add(classandother[0].substring(1));

            //【0】是pkg单独取出，【1】是其父类（用.连接），可能无
            if(classandother.length>1) {
                System.out.print("ent=" + ent + ",classandother=" + classandother[0] + "," + classandother[1]);
                String[] data = classandother[1].split("\\.");
                for (String s : data) {
                    if (!s.equals("")) {
                        set.add(s);
                    }
                }
            }



        }
//        System.out.print("callMap=====");
//        for(String s:callMap.keySet()){
//            System.out.println(s+":"+callMap.get(s));
//        }






        //2.Inclusion-callSet:callSet集合是在函数中的实体，即ent在函数中
        //callSet是除了函数里面包含的实体（但是不包含其调用的函数）
        for(String e:callSet){
            //m_BaseInstance:.Srccode.setBaseInstance_.Srccode.setBaseInstance_.Srccode.setBaseInstanceFromFileQ(2个父母)
            String[] allparents=callMap.get(e).split("_");
            for(String parent:allparents){

                String[]data=parent.split("\\.");
                for(String s:data){
                    if(s.equals(ent)){
                        set.add(e);
                    }
                }
            }


        }
//            for (String var:variableName) {
//                //找到其父母为ent
//                VariableDeclarationExpr v = variableMap.get(var);
//
//                if (v != null) {
//                    String[] data = JavaParserUtils.getParents(v).split("\\.");
//                    for (String s : data) {
//                        if (s.equals(ent)) {
//                        set.add(var);
//                    }
////                    if (!s.equals("")) {
////                        set.add(s);
////                    }
//
////                        System.out.println("========" + s);
//                    }
//
//                }
//            }

        for (String method:methodName) {
            //找到其父母为ent
            MethodDeclaration m = methodMap.get(method);

            if (m != null) {
//                System.out.println("[method]====="+method+" [parent]======");
                String[] data = JavaParserUtils.getParents(m).split("\\.");

                for (String s : data) {
                    if (s.equals(ent)) {
                        set.add(method);
                    }
//                    if (!s.equals("")) {
//                        set.add(s);
//                    }

//                    System.out.println("========" + s);
                }

            }
        }
        for (String insidemethod:insideMethodMap.keySet()) {
            //找到其父母为ent
            String[] allparents=insideMethodMap.get(insidemethod).split("_");
            for(String parent:allparents){

                String[]data=parent.split("\\.");
                for(String s:data){
                    if(s.equals(ent)){
                        set.add(insidemethod);
                    }
                }
            }
        }





        //3.
        ArrayList<String> res=new ArrayList<>();
        res.addAll(set);
//        String[] res = set.toArray(new String[set.size()]);
        //String[] res_={ "[m_BaseInstPanelCase]", "[setBaseInstanceFromFileQCase]","[setBaseInstancesFromDBQCase]"};
        return res;
    }

    public static  Vector<idenDS> ObtainIdentifier(Vector<String> allstate, String javafilepath) throws Exception
    {
        System.out.println(javafilepath);
        Vector<IdentifierDS> packages=new Vector<IdentifierDS>();  //加入当前的package
        Vector<IdentifierDS> types=new Vector<IdentifierDS>();     //类，接口，枚举
        Vector<IdentifierDS> methods=new Vector<IdentifierDS>();   //method，包括了constructor,setter,getter
        Vector<IdentifierDS> fields=new Vector<IdentifierDS>();    //
        Vector<IdentifierDS> variables=new Vector<IdentifierDS>(); //包括了函数的参数

        CompilationUnit cu =null;

        try {
            cu = JavaParser.parse(new File(javafilepath));

        }
        catch(Exception e)
        {
            System.err.println(e.toString());
//			BufferedWriter bw=new BufferedWriter(new FileWriter("D:\\project\\IdentifierStyle\\data\\JavaParserCannotParse.txt",true));
//            bw.write(javafilepath);
//            bw.newLine();
//            bw.close();
        }

        String packagename="";
        try {
            Optional<PackageDeclaration> packagename1=cu.getPackageDeclaration();
            if(packagename1.isPresent())
            {

                packagename=packagename1.get().toString().trim();
                if(packagename.startsWith("/*"))
                    packagename=packagename.substring(packagename.indexOf("*/")+2, packagename.length()).trim();
                if(packagename.startsWith("//"))
                    packagename=packagename.substring(packagename.indexOf("package "),packagename.length());
                if(packagename.startsWith("package "))
                    packagename=packagename.substring(packagename.indexOf(" ")+1,packagename.length());
                if(packagename.endsWith(";"))
                    packagename=packagename.substring(0,packagename.length()-1);
            }

        }
        catch(Exception e)
        {
            System.err.println(e.toString());
        }
        int packloc=1;
        for(int i=0;i<allstate.size();i++)
        {
            if(allstate.get(i).trim().startsWith("package "))
            {
                packloc=i+1;
                break;
            }
        }

        IdentifierDS newpackage=new IdentifierDS("","",packagename,"","",packloc);
        packages.add(newpackage);


        Hashtable<String,Integer> variableSet =new Hashtable<String,Integer>();	    	//所有的变量和对象
        VoidVisitor<Hashtable<String, Integer>> VariableCollector = new VariableCollector();
        try {
            VariableCollector.visit(cu, variableSet);
        }
        catch(Exception e)
        {
            System.err.println(e.toString());
        }


        Vector<ClassDS> classdetails=new Vector<ClassDS>();
        VoidVisitor<Vector<ClassDS>> classNameCollector = new ClassCollector();
        try {
            classNameCollector.visit(cu, classdetails);
        }
        catch(Exception e)
        {
            System.err.println(e.toString());
        }


        for(ClassDS one :classdetails)
        {
            IdentifierDS newclass=new IdentifierDS(one.getClassname(),"",one.getClassname(),"class","",one.getIndex());
            types.add(newclass);

            Vector<MethodDS> methodlist=one.getMethodlist();
            for(MethodDS onemethod:methodlist)
            {
                IdentifierDS newmethod=new IdentifierDS(one.getClassname(),onemethod.getMethodname(),onemethod.getMethodname(),onemethod.getReturntype(),"",onemethod.getBeginindex());
                methods.add(newmethod);

                Vector<IdentifierDS> parameters=onemethod.getParameters();
                for(IdentifierDS oneid:parameters)
                {
                    variables.add(oneid);
                }

            }

            Vector<IdentifierDS> fieldlist=one.getFieldlist();
            fields.addAll(fieldlist);

        }


        Set<String> keyset=variableSet.keySet();
        for(String onekey:keyset)
        {
            int onevalue=variableSet.get(onekey);
            String methodpar="";
            String classpar="";
            for(ClassDS one :classdetails)
            {

                Vector<MethodDS> methodlist=one.getMethodlist();
                for(MethodDS onemethod:methodlist)
                {
                    if(onevalue>=onemethod.getBeginindex()&&onevalue<=onemethod.getEndindex())
                    {
                        methodpar=onemethod.getMethodname();
                        classpar=one.getClassname();
                        break;
                    }
                }
            }


            if(onekey.contains("="))
            {
                String front=onekey.substring(0, onekey.indexOf("=")).trim();
                String end=onekey.substring(onekey.indexOf("=")+1, onekey.length()).trim();
                String name=front.substring(front.lastIndexOf(" ")+1, front.length()).trim();
                String type="";

                if(front.contains(" "))
                {
                    front=front.substring(0, front.lastIndexOf(" "));
                    if(front.contains(" "))
                    {
                        type=front.substring(front.lastIndexOf(" ")+1,front.length()).trim();
                    }
                    else type=front;
                }
                else
                    type=front;

                type=type.trim();
                IdentifierDS oneid=new IdentifierDS(classpar,methodpar,name,type,end,onevalue);

                variables.add(oneid);


            }
            else
            {
                String name=onekey.substring(onekey.lastIndexOf(" ")+1, onekey.length()).trim();
                onekey=onekey.substring(0, onekey.lastIndexOf(" ")).trim();
                String type="";
                if(onekey.contains(" "))
                    type=onekey.substring(onekey.lastIndexOf(" ")+1, onekey.length()).trim();
                else
                    type=onekey;

                type=type.trim();

                IdentifierDS oneid=new IdentifierDS(classpar,methodpar,name,type,"",onevalue);

                variables.add(oneid);

            }
        }


        Vector<idenDS> allid=new Vector<idenDS>();
        for(IdentifierDS onepackage:packages)
        {
//        	System.out.println(onepackage.toString());
            String identifiername=onepackage.getName();
            int location=onepackage.getLocation();
            String singlestate="";
            int purelocation=-1;
            if(location-1>=0&&allstate.size()>0)
            {
                singlestate=allstate.get(location-1);
                purelocation=location-1;
            }

            if(singlestate.contains(identifiername))
            {
                idenDS oness=new idenDS(1,identifiername,singlestate,purelocation);
                allid.add(oness);
            }
            else
            {
                System.err.println("1: 标识符位置不对！"+identifiername+"  "+singlestate);
            }

        }
        for(IdentifierDS onetype:types)
        {
//        	System.out.println(onetype.toString());

            String identifiername=onetype.getName();
            int location=onetype.getLocation();
            location=location/100000;

            String singlestate="";
            int purelocation=-1;
            if(location-1>=0&&allstate.size()>0)
            {
                singlestate=allstate.get(location-1);
                purelocation=location-1;
            }

            if(singlestate.trim().startsWith("@"))
            {
                if(location<allstate.size())
                {
                    singlestate=allstate.get(location);
                    purelocation=location;
                }

                if(singlestate.trim().startsWith("@"))
                {
                    singlestate=allstate.get(location+1);
                    purelocation=location+1;
                }
            }

            if(singlestate.contains(identifiername))
            {
                idenDS oness=new idenDS(2,identifiername,singlestate,purelocation);
                allid.add(oness);
            }
            else
            {
                System.err.println("2: 标识符位置不对！"+identifiername+"  "+singlestate);
            }

        }
        for(IdentifierDS onemethod:methods)
        {
            String identifiername=onemethod.getName();
            int location=onemethod.getLocation();

            String singlestate=allstate.get(location-1);
            int purelocation=location-1;

            if(singlestate.trim().startsWith("@"))
            {
                singlestate=allstate.get(location);
                purelocation=location;

                if(singlestate.trim().startsWith("@"))
                {
                    singlestate=allstate.get(location+1);
                    purelocation=location+1;
                }
            }

            if(singlestate.contains(identifiername))
            {
                idenDS oness=new idenDS(3,identifiername,singlestate,purelocation);
                allid.add(oness);
            }
            else
            {
                System.err.println("3: 标识符位置不对！"+identifiername+"  "+singlestate);
            }
        }
        for(IdentifierDS onefield:fields)
        {
            String identifiername=onefield.getName();
            int location=onefield.getLocation();
            String singlestate=allstate.get(location-1);
            int purelocation=location-1;
            if(singlestate.contains(identifiername))
            {
                idenDS oness=new idenDS(4,identifiername,singlestate,purelocation);
                allid.add(oness);
            }
            else
            {
                System.err.println("4: 标识符位置不对！"+identifiername+"  "+singlestate);
            }
        }
        for(IdentifierDS onevariable:variables)
        {
            String identifiername=onevariable.getName();
            int location=onevariable.getLocation();
            String singlestate=allstate.get(location-1);
            int purelocation=location-1;
            if(singlestate.trim().startsWith("@"))
            {
                singlestate=allstate.get(location);
                purelocation=location;
                if(singlestate.trim().startsWith("@"))
                {
                    singlestate=allstate.get(location+1);
                    purelocation=location+1;
                }
            }

            if(singlestate.contains(identifiername))
            {
                idenDS oness=new idenDS(5,identifiername,singlestate,purelocation);
                allid.add(oness);
            }
            else
            {
                if(purelocation+1<allstate.size())
                {
                    singlestate=singlestate+" "+allstate.get(purelocation+1);
                    //        		purelocation=location+1;
                    if(singlestate.contains(identifiername))
                    {
                        idenDS oness=new idenDS(5,identifiername,singlestate,purelocation);
                        allid.add(oness);
                    }
                    else
                    {
                        System.err.println("5: 标识符位置不对！"+identifiername+"  "+singlestate);
                    }
                }
            }
        }


        return allid;

    }
    public static void ExecuteCommand(String projectdir,String cmd,String output) throws Exception
    {
//        System.out.println("projectdir:"+projectdir);

        String[] command =
                {
                        "cmd",
                };
        Process p = Runtime.getRuntime().exec(command);
        new Thread(new SyncPipe(p.getErrorStream(), System.err)).start();
        new Thread(new SyncPipe(p.getInputStream(), System.out)).start();
        PrintWriter stdin = new PrintWriter(p.getOutputStream());

        stdin.println(projectdir.split("/")[0]);
        stdin.println("cd "+projectdir);
        stdin.println(cmd+" > "+output);
        //stdin.println("git log >"+output);
        stdin.close();
        int returnCode = p.waitFor();
//        System.out.println("Return code = " + returnCode);
    }
}
class commitMessage
{
    //获取得到的 id，author,date,message,diff显示部分（diff list）
    String commitid;
    String author;
    String date;
    String message;

    String historicalStmt;
    String curStmt;
    Vector<Diff> difflist=new Vector<Diff>();
    public Vector<Diff> getDifflist() {
        return difflist;
    }
    public String getCommitid() {
        return commitid;
    }
    public void setCommitid(String commitid) {
        this.commitid = commitid;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }


    public String getHistoricalStmt() {
        return historicalStmt;
    }

    public String getCurStmt() {
        return curStmt;
    }

    public commitMessage(String commitid, String author, String date, String message, Vector<Diff> difflist ,String historicalStmt, String curStmt) {
        super();
        this.commitid = commitid;
        this.author = author;
        this.date = date;
        this.message = message;

        this.historicalStmt=historicalStmt;
        this.curStmt=curStmt;
        this.difflist = difflist;

    }
    @Override
    public String toString() {
        return "commitMessage [commitid=" + commitid + ", author=" + author + ", date=" + date + ", message=" + message
                +  "]";
    }



}
class Diff {
    String fromFile;
    String toFile;
    String index;
    Vector<String> content = new Vector<String>();

    //文件
    public String getFromFile() {
        return fromFile;
    }

    public void setFromFile(String fromFile) {
        this.fromFile = fromFile;
    }

    public String getToFile() {
        return toFile;
    }

    public void setToFile(String toFile) {
        this.toFile = toFile;
    }

    //比较的行数
    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Vector<String> getContent() {
        return content;
    }

    public void setContent(Vector<String> content) {
        this.content = content;
    }

    public Diff(String fromFile, String toFile, String index, Vector<String> content) {
        super();
        this.fromFile = fromFile;
        this.toFile = toFile;
        this.index = index;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Diff [fromFile=" + fromFile + ", toFile=" + toFile + ", index=" + index + ", content=" + content.toString() + "]";
    }

}
class idenDS
{
    int type;
    String identifier;
    String statement;
    int location;
    String classpar;
    String methodpar;
    String IdType;

    public String getClasspar() {
        return classpar;
    }

    public void setClasspar(String classpar) {
        this.classpar = classpar;
    }

    public String getMethodpar() {
        return methodpar;
    }

    public void setMethodpar(String methodpar) {
        this.methodpar = methodpar;
    }

    public String getIdType() {
        return IdType;
    }

    public void setIdType(String idType) {
        IdType = idType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    String defaultValue;
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public String getStatement() {
        return statement;
    }
    public void setStatement(String statement) {
        this.statement = statement;
    }
    public int getLocation() {
        return location;
    }
    public void setLocation(int location) {
        this.location = location;
    }
    public idenDS(int type, String identifier, String statement, int location) {
        super();
        this.type = type;
        this.identifier = identifier;
        this.statement = statement;
        this.location = location;
    }
    public idenDS(int type, String identifier, String statement, int location,String classpar,String methodpar,String IdType,String defaultValue) {
        super();
        this.type = type;
        this.identifier = identifier;
        this.statement = statement;
        this.location = location;
        this.classpar=classpar;
        this.methodpar=methodpar;
        this.IdType=IdType;
        this.defaultValue=defaultValue;
    }

    @Override
    public String toString() {
        return "idenDS [type=" + type + ", identifier=" + identifier + ", statement=" + statement + ", location="
                + location + "]";
    }

}

