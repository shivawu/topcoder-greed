package greed;

import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.problem.Renderer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import greed.code.CodeByLine;
import greed.code.LanguageManager;
import greed.model.*;
import greed.template.TemplateEngine;
import greed.ui.ConfigurationDialog;
import greed.ui.GreedEditorPanel;
import greed.util.Configuration;
import greed.util.FileSystem;
import greed.util.Log;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static greed.util.Configuration.Keys;

/**
 * Greed is good! Cheers!
 */
@SuppressWarnings("unused")
public class Greed {
    public static final String APP_NAME = "Greed";
    public static final String APP_VERSION = "v1.2";

    private Language currentLang;
    private Problem currentProb;
    private Contest currentContest;
    private HashMap<String, Object> currentTemplateModel;

    private GreedEditorPanel talkingWindow;
    private boolean firstUsing;

    public Greed() {
        // Entrance of all program
        Log.i("Create Greed Plugin");
        this.talkingWindow = new GreedEditorPanel(this);
        this.firstUsing = true;
    }

    // Greed signature in the code
    public String getSignature() {
        return String.format("%s Powered by %s %s",
                LanguageManager.getInstance().getTrait(currentLang).getCommentPrefix(), APP_NAME, APP_VERSION);
    }

    // Cache the editor
    public boolean isCacheable() {
        return false;
    }

    // Called when open the coding frame
    // Like FileEdit, a log window is used
    public JPanel getEditorPanel() {
        return talkingWindow;
    }

    // Ignore the given source code
    public void setSource(String source) {
    }

    public String getSource() {
        String codeDir = Configuration.getString(Keys.CODE_ROOT);
        String relativePath = TemplateEngine.render(Configuration.getString(Keys.PATH_PATTERN), currentTemplateModel);
        String fileName = TemplateEngine.render(Configuration.getString(Keys.FILE_NAME_PATTERN), currentTemplateModel);

        // Create source file if not exists
        Config langSpecConfig = Configuration.getConfig(Keys.getTemplateKey(currentLang));
        String filePath = codeDir + "/" + relativePath + "/" + fileName + "." + langSpecConfig.getString(Keys.SUBKEY_EXTENSION);

        // Get begincut and endcut tag
        String beginCut = langSpecConfig.getString(Keys.SUBKEY_CUTBEGIN);
        String endCut = langSpecConfig.getString(Keys.SUBKEY_CUTEND);

        try {
            CodeByLine code = CodeByLine.fromInputStream(FileSystem.getInputStream(filePath));

            if (LanguageManager.getInstance().getProcessor(currentLang) != null)
                code = LanguageManager.getInstance().getProcessor(currentLang).process(code);

            // Cut the code
            boolean cutting = false;
            StringBuffer buf = new StringBuffer();
            for (String line : code.getLines()) {
                if (line.equals(beginCut))
                    cutting = true;
                else if (line.equals(endCut))
                    cutting = false;
                else if (!cutting) {
                    buf.append(line);
                    buf.append("\n");
                }
            }

            buf.append(getSignature());
            return buf.toString();
        } catch (IOException e) {
            talkingWindow.say("Errr... Cannot fetch your source code. Please check the logs, and make sure your source code is present");
            talkingWindow.say("Now I'm giving out a empty string!");
            Log.e("Error getting the source", e);
            return "";
        }
    }

    public void startUsing() {
        Log.i("Start using called");
        talkingWindow.clear();
        if (firstUsing) {
            talkingWindow.say(String.format("Hi, this is %s.", APP_NAME));
        } else {
            talkingWindow.say(String.format("So we meet again :>"));
        }
        firstUsing = false;
    }

    public void stopUsing() {
        Log.i("Stop using called");
    }

    public void configure() {
        new ConfigurationDialog().setVisible(true);
    }

    public void setProblemComponent(ProblemComponentModel componentModel, com.topcoder.shared.language.Language language, Renderer renderer) {
        currentContest = Convert.convertContest(componentModel);
        currentLang = Convert.convertLanguage(language);
        currentProb = Convert.convertProblem(componentModel, currentLang);

        talkingWindow.say("Hmmm, it's a problem with " + currentProb.getScore() + " points. Good choice!");

        generateCode();
    }

    public void generateCode() {
        // Check whether workspace is set
        if (Configuration.getWorkspace() == null || "".equals(Configuration.getWorkspace())) {
            talkingWindow.setEnabled(false);
            talkingWindow.say("It seems that you haven't set your workspace, go set it!");
            Log.e("Workspace not set");
            return;
        }

        talkingWindow.setEnabled(true);
        try {
            setProblem(currentContest, currentProb, currentLang);
        } catch (Throwable e) {
            talkingWindow.say("Oops, something wrong! It says \"" + e.getMessage() + "\"");
            talkingWindow.say("Please see the logs for details.");
            Log.e("Set problem failed", e);
        }
    }

    private void setProblem(Contest contest, Problem problem, Language language) {
        Config langSpecConfig = Configuration.getConfig(Keys.getTemplateKey(language));
        TemplateEngine.switchLanguage(language);

        // Create model map
        currentTemplateModel = new HashMap<String, Object>();
        currentTemplateModel.put("Contest", contest);
        currentTemplateModel.put("Problem", problem);

        // Create source directory
        String codeDir;
        {
            String codeRoot = Configuration.getString(Keys.CODE_ROOT);
            String relativePath = TemplateEngine.render(Configuration.getString(Keys.PATH_PATTERN), currentTemplateModel);
            codeDir = codeRoot + "/" + relativePath;

            if (!FileSystem.exists(codeDir)) {
                talkingWindow.say("I'm creating folder " + codeDir);
                FileSystem.createFolder(codeDir);
            }
        }

        boolean override = Configuration.getBoolean(Keys.OVERRIDE);
        String sourceFilePath;
        {
            String fileName = TemplateEngine.render(Configuration.getString(Keys.FILE_NAME_PATTERN), currentTemplateModel);
            sourceFilePath = codeDir + "/" + fileName + "." + langSpecConfig.getString(Keys.SUBKEY_EXTENSION);
        }
        boolean sourceFileExists = FileSystem.exists(sourceFilePath);

        boolean doUnitTest = Configuration.getBoolean(Keys.UNIT_TEST);
        String unitTestFilePath;
        {
            String unitTestFileName = TemplateEngine.render(Configuration.getString(Keys.UNIT_TEST_FILE_NAME_PATTERN), currentTemplateModel);
            unitTestFilePath = codeDir + "/" + unitTestFileName + "." + langSpecConfig.getString(Keys.SUBKEY_EXTENSION);
        }
        boolean unitTestFileExists = FileSystem.exists(unitTestFilePath);

        if (!sourceFileExists || (doUnitTest && !unitTestFileExists) || override) {
            currentTemplateModel.put("ClassName", problem.getClassName());
            currentTemplateModel.put("Method", problem.getMethod());
            currentTemplateModel.put("Examples", problem.getTestcases());
            currentTemplateModel.put("NumOfExamples", problem.getTestcases().length);
            boolean useArray = problem.getMethod().getReturnType().isArray();
            currentTemplateModel.put("ReturnsArray", useArray);
            for (Param param : problem.getMethod().getParams()) useArray |= param.getType().isArray();
            currentTemplateModel.put("HasArray", useArray);
            currentTemplateModel.put("RecordRuntime", Configuration.getBoolean(Keys.RECORD_RUNTIME));
            currentTemplateModel.put("RecordScore", Configuration.getBoolean(Keys.RECORD_SCORE));
            currentTemplateModel.put("CreateTime", System.currentTimeMillis() / 1000);
            currentTemplateModel.put("CutBegin", langSpecConfig.getString(Keys.SUBKEY_CUTBEGIN));
            currentTemplateModel.put("CutEnd", langSpecConfig.getString(Keys.SUBKEY_CUTEND));
        }

        // Create source file if not exists
        talkingWindow.say("Source code will be generated, \"" + sourceFilePath + "\"" + ", in your workspace");
        if (sourceFileExists) {
            talkingWindow.say("Seems the file has been created");
        }
        if (sourceFileExists && !override) {
            // Skip old files due to override options
            talkingWindow.say("This time I'll not override it, if you say so.");
        } else {
            talkingWindow.say("I'm generating source code for you~");
            generateSourceFile(sourceFilePath, sourceFileExists, doUnitTest, langSpecConfig);
        }

        if (doUnitTest) {
            // Create unit test file if not exists
            talkingWindow.say("Unit test code will be generated, \"" + unitTestFilePath + "\"" + ", in your workspace");
            if (unitTestFileExists) {
                talkingWindow.say("Seems the file has been created");
            }
            if (unitTestFileExists && !override) {
                // Skip old files due to override options
                talkingWindow.say("This time I'll not override it, if you say so.");
            } else {
                talkingWindow.say("I'm generating unit test code for you~");
                generateUnitTestFile(unitTestFilePath, unitTestFileExists, langSpecConfig);
            }
        }

        talkingWindow.say("All set, good luck!");
        talkingWindow.say("");
    }

    private void generateUnitTestFile(String unitTestFilePath, boolean unitTestFileExists, Config langSpecConfig) {
        String sourceCode;
        try {
            String tmplPath = langSpecConfig.getString(Keys.SUBKEY_UNIT_TEST_TEMPLATE_FILE);
            talkingWindow.say("Using unit test template \"" + tmplPath + "\"");

            InputStream codeTmpl = FileSystem.getInputStream(tmplPath);
            sourceCode = TemplateEngine.render(codeTmpl, currentTemplateModel);
        } catch (FileNotFoundException e) {
            talkingWindow.say("No unit test template, no testing code for you.");
            Log.w("Unit test template not found, probably because user specify a non-exist testing template, resulting code without testing module");
            return;
        } catch (ConfigException e) {
            talkingWindow.say("What's that about the unit test template? I didn't understand.");
            Log.w("Incorrect unit test template configuration", e);
            return;
        }

        if (unitTestFileExists) {
            talkingWindow.say("Overriding, old files will be renamed");
            if (FileSystem.getSize(unitTestFilePath) == sourceCode.length()) {
                talkingWindow.say("Seems the current file is the same as the template.");
                talkingWindow.say("OK, just use the current file, I'm not writing to it.");
                return;
            } else
                FileSystem.backup(unitTestFilePath); // Backup the old files
        }

        // Write to file
        FileSystem.writeFile(unitTestFilePath, sourceCode);
    }

    private void generateSourceFile(String sourceFilePath, boolean sourceFileExists, boolean doUnitTest, Config langSpecConfig) {
        // Generate embedded test code
        if (!doUnitTest) {
            try {
                String tmplPath = langSpecConfig.getString(Keys.SUBKEY_TEST_TEMPLATE_FILE);
                talkingWindow.say("Using test template \"" + tmplPath + "\"");

                InputStream testTmpl = FileSystem.getInputStream(tmplPath);
                String testCode = TemplateEngine.render(testTmpl, currentTemplateModel);
                currentTemplateModel.put("TestCode", testCode);
            } catch (FileNotFoundException e) {
                talkingWindow.say("No testing template, no testing code for you.");
                Log.w("Testing template not found, probably because user specify a non-exist testing template, resulting code without testing module");
            } catch (ConfigException e) {
                talkingWindow.say("What's that about the testing template? I didn't understand.");
                Log.w("Incorrect test template configuration", e);
            }
        } else {
            currentTemplateModel.put("TestCode", "");
        }

        // Generate main code
        String sourceCode;
        try {
            String tmplPath = langSpecConfig.getString(Keys.SUBKEY_TEMPLATE_FILE);
            talkingWindow.say("Using source template \"" + tmplPath + "\"");

            InputStream codeTmpl = FileSystem.getInputStream(tmplPath);
            sourceCode = TemplateEngine.render(codeTmpl, currentTemplateModel);
        } catch (FileNotFoundException e) {
            talkingWindow.say("Oh no, where's your source code template?");
            talkingWindow.say("You have to start with a empty file yourself :<");
            Log.e("Source code template not found, this is fatal error, source code will not be generated");
            return;
        } catch (ConfigException e) {
            talkingWindow.say("No configuration for source code template, I'm giving up!");
            Log.e("Incorrect code template configuration", e);
            return;
        }

        if (sourceFileExists) {
            talkingWindow.say("Overriding, old files will be renamed");
            if (FileSystem.getSize(sourceFilePath) == sourceCode.length()) {
                talkingWindow.say("Seems the current file is the same as the template.");
                talkingWindow.say("OK, just use the current file, I'm not writing to it.");
                return;
            } else
                FileSystem.backup(sourceFilePath); // Backup the old files
        }

        // Write to file
        FileSystem.writeFile(sourceFilePath, sourceCode);
    }
}
