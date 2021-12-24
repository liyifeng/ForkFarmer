package forks;

import main.ForkFarmer;
import util.Ico;
import util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.*;

/**
 * @author liyifeng
 * @version 1.0
 * @date 2021/12/24 3:30 下午
 */
public class AncillaryFunction {
    public static final ImageIcon HELP = Ico.loadIcon("icons/question_16x16.png", 16);
    public static final ImageIcon SHOW_PLOT_DIR = Ico.loadIcon("util/jtattoo/icons/small/folder_opened_16x16.png");

    public static void importKeysFromChia(Fork fork, boolean showResultMsg) {
        ForkFarmer.LOG.add("开始向[" + fork.name + "]导入助记词");
        Fork fromFork = getChia();
        String chiaKeyResponseString = Util.runProcessWait(fromFork.exePath, "keys", "show", "--show-mnemonic-seed");
        List<Map<String, String>> keywordList = getKeys(chiaKeyResponseString);
        ForkFarmer.LOG.add("从Chia找到了助记词数量:" + keywordList.size());
        int findKeyCount = 0;
        for (Map<String, String> wordMap : keywordList) {
            ForkFarmer.LOG.add("助记词:" + wordMap.get("Fingerprint"));
        }

        List<Map<String, Object>> resultMapList = new ArrayList<>();

        for (Map<String, String> k : keywordList) {
            String fingerprint = k.get("Fingerprint");
            String words = k.get("words");
            Map<String, Object> result = new HashMap<>();

            result.put("Fingerprint", fingerprint);

            ForkFarmer.LOG.add("导入助记词开始:" + fingerprint);
            // save 2 file
            File tempFile = new File(System.getProperty("user.home") + File.separator + ".forkFarmer_temp" + File.separator + fingerprint + ".temp");
            String keyFilePath = tempFile.getAbsolutePath();
            //LOGGER.info("创建助记词临时文件{},{}", fingerprint, keyFilePath);
            try {
                if (!tempFile.getParentFile().exists()) {
                    tempFile.getParentFile().mkdirs();
                }
                if (tempFile.exists()) {
                    tempFile.delete();
                } else {
                    tempFile.createNewFile();
                    FileWriter fw = new FileWriter(tempFile);
                    fw.write(words);
                    fw.flush();
                    fw.close();
                }

                String importResult = Util.runProcessWait(fork.exePath, "keys", "add", "-f", keyFilePath);
                boolean importSuccess = importResult != null && importResult.indexOf(fingerprint) > -1;
                ForkFarmer.LOG.add("执行命令导入助记词[" + fingerprint + "]结果：" + (importSuccess ? "成功" : "失败"));
                result.put("importSuccess", importSuccess ? "导入成功" : "导入失败");
                resultMapList.add(result);

                boolean delSuccess = tempFile.delete();
                if (delSuccess) {
                    //LOGGER.info("导入助记词所有任务结束，删除助记词临时文件：{}", delSuccess);
                } else {
                    ForkFarmer.LOG.add("自动删除临时文件失败，最好能手动删除该文件:" + keyFilePath);
                }
            } catch (IOException e) {
                ForkFarmer.LOG.add("向临时文件写入助记词内容报错:" + keyFilePath);
            }
        }

        String showMsg = "";
        if (resultMapList.size() > 0) {
            for (Map<String, Object> map : resultMapList) {
                showMsg += "助记词[" + map.get("Fingerprint") + "] " + map.get("importSuccess") + "\n";
            }
        } else {
            if (findKeyCount == 0) {
                showMsg = "从Chia币客户端没有发现任何助记词信息";
            } else {
                showMsg = "从Chia币客户端找到了" + findKeyCount + "个助记词，但是导入失败，请logs目录下日志查找失败原因";
            }
        }
        if (showResultMsg) {
            ForkFarmer.showMsg("导入结果", showMsg);
        }
    }

    private static List<Map<String, String>> getKeys(String lineString) {
        List<Map<String, String>> list = new ArrayList<>();
        if (lineString != null) {
            boolean startFlag = false;
            String[] lines = lineString.split("\\r?\\n");
            Map<String, String> keyword = null;
            for (String line : lines) {
                if (line != null && line.length() > 0 && line.trim().length() > 0) {
                    if (line.startsWith("Fingerprint: ")) {
                        String Fingerprint = line.substring("Fingerprint: ".length());
                        keyword = new HashMap<>(2);
                        list.add(keyword);
                        keyword.put("Fingerprint", Fingerprint);
                    }
                    if (startFlag) {
                        keyword.put("words", line.trim());
                        startFlag = false;
                    }
                    if (line.indexOf("Mnemonic seed (24 secret words):") > -1) {
                        startFlag = true;
                    }

                }
//                System.out.println(line);
            }
        }

        return list;
    }

    public static void mountChiaPlotDir2Current(Fork fork, boolean showResultMsg) {
        ForkFarmer.LOG.add(String.format("从Chia拷贝农田目录到[%s]开始...", fork.name));
        Fork fromFork = getChia();
        String lineString = Util.runProcessWait(fromFork.exePath, "plots", "show");
        String showMsg = "";
        if (lineString != null) {
            List<String> chiaPlotDirs = getPlotDirs(lineString);
            ForkFarmer.LOG.add(String.format("从Chia拷贝农田目录到[%s],Chia的农田目录数:%d", fork.name, chiaPlotDirs.size()));
            showMsg += "Chia农田目录数量:" + chiaPlotDirs.size() + "\n";
            String myPlots = Util.runProcessWait(fork.exePath, "plots", "show");
            List<String> myPlotDirList = getPlotDirs(myPlots);
            ForkFarmer.LOG.add(String.format("从Chia拷贝农田目录到[%s],[%s]的农田目录数:%d", fork.name, fork.name, myPlotDirList.size()));
            showMsg += fork.name + "的农田目录数量:" + myPlotDirList.size() + "\n";

            Set<String> chiaPlotDirSet = new LinkedHashSet<>(chiaPlotDirs);
            Set<String> myPlotDirSet = new LinkedHashSet<>(myPlotDirList);
            int add = 0, del = 0, keep = 0;
            for (String chiaPlotDir : chiaPlotDirs) {
                if (myPlotDirSet.contains(chiaPlotDir)) {
                    keep++;
                    ForkFarmer.LOG.add(String.format("[%s]已经有:%s,跳过", fork.name, chiaPlotDir));
                } else {
                    add++;
                    String res = Util.runProcessWait(fork.exePath, "plots", "add", "-d", chiaPlotDir);
                    ForkFarmer.LOG.add(String.format("[%s]添加目录:%s", fork.name, chiaPlotDir));
                }
            }

            for (String myPlotDir : myPlotDirList) {
                if (chiaPlotDirSet.contains(myPlotDir)) {

                } else {
                    del++;
                    String res = Util.runProcessWait(fork.exePath, "plots", "remove", "-d", myPlotDir);
                    ForkFarmer.LOG.add(String.format("[%s]移除目录:%s", fork.name, myPlotDir));
                }
            }
            showMsg += fork.name + "新增目录：" + add + "\n";
            showMsg += fork.name + "删除目录：" + del + "\n";
            showMsg += fork.name + "保持没变：" + keep + "\n";
            showMsg += "====================================\n";
            ForkFarmer.LOG.add(String.format("[%s]最终的最终导入结果，新增：%d", fork.name, add));
            ForkFarmer.LOG.add(String.format("[%s]最终的最终导入结果，删除：%d", fork.name, del));
            ForkFarmer.LOG.add(String.format("[%s]最终的最终导入结果，保持没变：%d", fork.name, keep));
            if (showResultMsg) {
                String myPlots2 = Util.runProcessWait(fork.exePath, "plots", "show");
                List<String> myPlotDirList2 = getPlotDirs(myPlots2);
                showMsg += fork.name + "最终的农田目录是：\n";
                for (String dir : myPlotDirList2) {
                    showMsg += dir + "\n";
                }
            }

        } else {
            showMsg = "获取Chia农田目录失败。";
            ForkFarmer.LOG.add("从Chia获取农田目录失败");
        }
        ForkFarmer.LOG.add(String.format("从Chia拷贝农田目录到[%s]结束", fork.name));
        if (showResultMsg) {
            ForkFarmer.showMsg(fork.name + "导入农田结果", showMsg);
        }
    }

    private static Fork getChia() {
        Fork fromFork = null;
        for (Fork fork1 : Fork.LIST) {
            if (fork1.symbol.equalsIgnoreCase("xch")) {
                fromFork = fork1;
                break;
            }
        }
        return fromFork;
    }

    private static List<String> getPlotDirs(String lineString) {
        boolean startFlag = false;
        List<String> plotDirs = new ArrayList<>();
        String[] lines = lineString.split("\\r?\\n");
        for (String line : lines) {
            if (line != null && line.trim().length() > 0) {
                if (startFlag) {
                    plotDirs.add(line.trim());
                }
                if (!startFlag && line.startsWith("Add with")) {
                    startFlag = true;
                }
            }

        }
        Collections.sort(plotDirs);
        return plotDirs;
    }

    public static void showPlotDirs(Fork fork) {
        try {
            String myPlots2 = Util.runProcessWait(fork.exePath, "plots", "show");
            List<String> myPlotDirList2 = getPlotDirs(myPlots2);
            String showMsg = fork.name + "当前的农田目录有" + myPlotDirList2.size() + "个，分别是：\n";
            int index = 0;
            for (String dir : myPlotDirList2) {
                File file = new File(dir);
                String tip = file.exists() ? "" : "（目录不存在）";
                showMsg += (++index) + ". " + dir + tip + "\n";
            }
            ForkFarmer.showMsg(fork.name + "农田目录", showMsg);

        } catch (Exception e) {
            //LOGGER.error("查看农田目录报错", e);
        }
    }

    public static void jump2HelpPage() {
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(new URI("https://www.c4dig.cn/page/2618.html?from=ff2.5.1"));
        } catch (Exception e) {
            e.printStackTrace();
            ForkFarmer.showMsg("获取帮助", "请访问：https://www.c4dig.cn/page/2618.html 获取帮助信息。");
        }
    }
}
