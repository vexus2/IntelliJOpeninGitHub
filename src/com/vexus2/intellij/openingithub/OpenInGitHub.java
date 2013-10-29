package com.vexus2.intellij.openingithub;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenInGitHub extends AnAction {

    public static final String GITHUB_URL = "https://github.com/";

    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(PlatformDataKeys.EDITOR.getData(e.getDataContext()) != null);
    }

    public void actionPerformed(AnActionEvent e) {

        Project p = e.getProject();

//        VirtualFile[] selectedFiles = FileEditorManager.getInstance(p).getSelectedFiles();
        VirtualFile current_file = (VirtualFile) PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());

//        String current_file_name = selectedFiles[0].toString().replace("file://" + p.getBasePath() + "/", "");
        String current_file_name = current_file.toString().replace("file://" + p.getBasePath() + "/", "");

        BufferedReader br_config;

        BufferedReader br_head;

        String line;
        String group = "";
        String project = "";
        String head = "";
        String cursor_line;
        boolean is_set_github = false;

        try {
            br_config = new BufferedReader(new FileReader(p.getBasePath() + "/.git/config"));
            br_head = new BufferedReader(new FileReader(p.getBasePath() + "/.git/HEAD"));

            while ((line = br_config.readLine()) != null) {
                if (line.matches(".*url = .*")) {
                    Pattern pattern = Pattern.compile("(git@github.com:(.*?)/(.*?).git|https://github.com/(.*?)/(.*?).git)");
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        group = (matcher.group(2) != null) ? matcher.group(2) : matcher.group(4);
                        project = (matcher.group(3) != null) ? matcher.group(3) : matcher.group(5);
                        is_set_github = true;
                        break;
                    }
                }
            }

            while ((line = br_head.readLine()) != null) {
                Pattern pattern = Pattern.compile(".*?heads(.*?)$");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    head = matcher.group(1);
                }
            }

        } catch (Exception exception) {
            // .git/config is empty or not found
            Notifications.Bus.notify(new Notification("Open in GitHub", "Error", "Can not open [.git/config] file.", NotificationType.ERROR));
            return;
        }

        if (!is_set_github) {
            // repository is not management in github
            Notifications.Bus.notify(new Notification("Open in GitHub", "Error", "Repository is not management in GitHub.", NotificationType.ERROR));
            return;
        }

        Editor editor = (Editor) PlatformDataKeys.EDITOR.getData(e.getDataContext());
        SelectionModel selectionModel = editor.getSelectionModel();


        if (selectionModel.hasSelection()) {
            cursor_line = "#L" + (selectionModel.getSelectionStartPosition().getLine() + 1) + "-L" + (selectionModel.getSelectionEndPosition().getLine() + 1);
        } else {
            cursor_line = "#L" + (selectionModel.getSelectionStartPosition().getLine() + 1);
        }

        String request = GITHUB_URL + group + "/" + project + "/blob" + head + "/" + current_file_name + cursor_line;

        String[] command = new String[]{ExecUtil.getOpenCommandPath()};

        try {
            final GeneralCommandLine commandLine = new GeneralCommandLine(command);
            commandLine.addParameter(request);
            commandLine.createProcess();

        } catch (ExecutionException exception) {
            Notifications.Bus.notify(new Notification("Open in GitHub", "Error", "Error: " + exception.getMessage(), NotificationType.ERROR));
            return;
        }
    }
}
