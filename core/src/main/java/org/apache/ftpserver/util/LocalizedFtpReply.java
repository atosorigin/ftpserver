/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ftpserver.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.interfaces.FtpIoSession;
import org.apache.ftpserver.interfaces.FtpServerContext;
import org.apache.ftpserver.message.MessageResource;

/**
 * FTP reply translator.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class LocalizedFtpReply extends DefaultFtpReply {

    private static final String CLIENT_ACCESS_TIME = "client.access.time";

    private static final String CLIENT_CON_TIME = "client.con.time";

    private static final String CLIENT_DIR = "client.dir";

    private static final String CLIENT_HOME = "client.home";

    private static final String CLIENT_IP = "client.ip";

    private static final String CLIENT_LOGIN_NAME = "client.login.name";

    private static final String CLIENT_LOGIN_TIME = "client.login.time";

    private static final String OUTPUT_CODE = "output.code";

    private static final String OUTPUT_MSG = "output.msg";

    private static final String REQUEST_ARG = "request.arg";

    private static final String REQUEST_CMD = "request.cmd";

    private static final String REQUEST_LINE = "request.line";

    private static final String SERVER_IP = "server.ip";

    private static final String SERVER_PORT = "server.port";

    private static final String STAT_CON_CURR = "stat.con.curr";

    private static final String STAT_CON_TOTAL = "stat.con.total";

    private static final String STAT_DIR_CREATE_COUNT = "stat.dir.create.count";

    private static final String STAT_DIR_DELETE_COUNT = "stat.dir.delete.count";

    private static final String STAT_FILE_DELETE_COUNT = "stat.file.delete.count";

    private static final String STAT_FILE_DOWNLOAD_BYTES = "stat.file.download.bytes";

    private static final String STAT_FILE_DOWNLOAD_COUNT = "stat.file.download.count";

    private static final String STAT_FILE_UPLOAD_BYTES = "stat.file.upload.bytes";

    private static final String STAT_FILE_UPLOAD_COUNT = "stat.file.upload.count";

    private static final String STAT_LOGIN_ANON_CURR = "stat.login.anon.curr";

    private static final String STAT_LOGIN_ANON_TOTAL = "stat.login.anon.total";

    private static final String STAT_LOGIN_CURR = "stat.login.curr";

    private static final String STAT_LOGIN_TOTAL = "stat.login.total";

    private static final String STAT_START_TIME = "stat.start.time";
    
    public static FtpReply translate(FtpIoSession session, FtpRequest request,
            FtpServerContext context, int code, String subId, String basicMsg) {
        String msg = translateMessage(session, request, context, code, subId,
                basicMsg);

        return new LocalizedFtpReply(code, msg);
    }

    private static String translateMessage(FtpIoSession session,
            FtpRequest request, FtpServerContext context, int code,
            String subId, String basicMsg) {
        MessageResource resource = context.getMessageResource();
        String lang = session.getLanguage();

        String msg = null;
        if (resource != null) {
            msg = resource.getMessage(code, subId, lang);
        }
        if (msg == null) {
            msg = "";
        }
        msg = replaceVariables(session, request, context, code, basicMsg, msg);

        return msg;
    }

    /**
     * Replace server variables.
     */
    private static String replaceVariables(FtpIoSession session,
            FtpRequest request, FtpServerContext context, int code,
            String basicMsg, String str) {

        int startIndex = 0;
        int openIndex = str.indexOf('{', startIndex);
        if (openIndex == -1) {
            return str;
        }

        int closeIndex = str.indexOf('}', startIndex);
        if ((closeIndex == -1) || (openIndex > closeIndex)) {
            return str;
        }

        StringBuffer sb = new StringBuffer(128);
        sb.append(str.substring(startIndex, openIndex));
        while (true) {
            String varName = str.substring(openIndex + 1, closeIndex);
            sb.append(getVariableValue(session, request, context, code,
                    basicMsg, varName));

            startIndex = closeIndex + 1;
            openIndex = str.indexOf('{', startIndex);
            if (openIndex == -1) {
                sb.append(str.substring(startIndex));
                break;
            }

            closeIndex = str.indexOf('}', startIndex);
            if ((closeIndex == -1) || (openIndex > closeIndex)) {
                sb.append(str.substring(startIndex));
                break;
            }
            sb.append(str.substring(startIndex, openIndex));
        }
        return sb.toString();
    }

    /**
     * Get the variable value.
     */
    private static String getVariableValue(FtpIoSession session,
            FtpRequest request, FtpServerContext context, int code,
            String basicMsg, String varName) {

        String varVal = null;

        // all output variables
        if (varName.startsWith("output.")) {
            varVal = getOutputVariableValue(session, code, basicMsg, varName);
        }

        // all server variables
        else if (varName.startsWith("server.")) {
            varVal = getServerVariableValue(session, varName);
        }

        // all request variables
        else if (varName.startsWith("request.")) {
            varVal = getRequestVariableValue(session, request, varName);
        }

        // all statistical variables
        else if (varName.startsWith("stat.")) {
            varVal = getStatisticalVariableValue(session, context, varName);
        }

        // all client variables
        else if (varName.startsWith("client.")) {
            varVal = getClientVariableValue(session, varName);
        }

        if (varVal == null) {
            varVal = "";
        }
        return varVal;
    }

    /**
     * Get client variable value.
     */
    private static String getClientVariableValue(FtpIoSession session,
            String varName) {

        String varVal = null;

        // client ip
        if (varName.equals(CLIENT_IP)) {
            if (session.getRemoteAddress() instanceof InetSocketAddress) {
                InetSocketAddress remoteSocketAddress = (InetSocketAddress) session
                        .getRemoteAddress();
                varVal = remoteSocketAddress.getAddress().getHostAddress();
            }

        }

        // client connection time
        else if (varName.equals(CLIENT_CON_TIME)) {
            varVal = DateUtils.getISO8601Date(session.getCreationTime());
        }

        // client login name
        else if (varName.equals(CLIENT_LOGIN_NAME)) {
            if (session.getUser() != null) {
                varVal = session.getUser().getName();
            }
        }

        // client login time
        else if (varName.equals(CLIENT_LOGIN_TIME)) {
            varVal = DateUtils.getISO8601Date(session.getLoginTime().getTime());
        }

        // client last access time
        else if (varName.equals(CLIENT_ACCESS_TIME)) {
            varVal = DateUtils.getISO8601Date(session.getLastAccessTime()
                    .getTime());
        }

        // client home
        else if (varName.equals(CLIENT_HOME)) {
            varVal = session.getUser().getHomeDirectory();
        }

        // client directory
        else if (varName.equals(CLIENT_DIR)) {
            FileSystemView fsView = session.getFileSystemView();
            if (fsView != null) {
                try {
                    varVal = fsView.getCurrentDirectory().getFullName();
                } catch (Exception ex) {
                    varVal = "";
                }
            }
        }
        return varVal;
    }

    /**
     * Get output variable value.
     */
    private static String getOutputVariableValue(FtpIoSession session,
            int code, String basicMsg, String varName) {
        String varVal = null;

        // output code
        if (varName.equals(OUTPUT_CODE)) {
            varVal = String.valueOf(code);
        }

        // output message
        else if (varName.equals(OUTPUT_MSG)) {
            varVal = basicMsg;
        }

        return varVal;
    }

    /**
     * Get request variable value.
     */
    private static String getRequestVariableValue(FtpIoSession session,
            FtpRequest request, String varName) {

        String varVal = null;

        if (request == null) {
            return "";
        }

        // request line
        if (varName.equals(REQUEST_LINE)) {
            varVal = request.getRequestLine();
        }

        // request command
        else if (varName.equals(REQUEST_CMD)) {
            varVal = request.getCommand();
        }

        // request argument
        else if (varName.equals(REQUEST_ARG)) {
            varVal = request.getArgument();
        }

        return varVal;
    }

    /**
     * Get server variable value.
     */
    private static String getServerVariableValue(FtpIoSession session,
            String varName) {

        String varVal = null;

        SocketAddress localSocketAddress = session.getLocalAddress();

        if (localSocketAddress instanceof InetSocketAddress) {
            InetSocketAddress localInetSocketAddress = (InetSocketAddress) localSocketAddress;
            // server address
            if (varName.equals(SERVER_IP)) {

                InetAddress addr = localInetSocketAddress.getAddress();

                if (addr != null) {
                    varVal = addr.getHostAddress();
                }
            }

            // server port
            else if (varName.equals(SERVER_PORT)) {
                varVal = String.valueOf(localInetSocketAddress.getPort());
            }
        }

        return varVal;
    }

    /**
     * Get statistical connection variable value.
     */
    private static String getStatisticalConnectionVariableValue(
            FtpIoSession session, FtpServerContext context, String varName) {
        String varVal = null;
        FtpStatistics stat = context.getFtpStatistics();

        // total connection number
        if (varName.equals(STAT_CON_TOTAL)) {
            varVal = String.valueOf(stat.getTotalConnectionNumber());
        }

        // current connection number
        else if (varName.equals(STAT_CON_CURR)) {
            varVal = String.valueOf(stat.getCurrentConnectionNumber());
        }

        return varVal;
    }

    /**
     * Get statistical directory variable value.
     */
    private static String getStatisticalDirectoryVariableValue(
            FtpIoSession session, FtpServerContext context, String varName) {
        String varVal = null;
        FtpStatistics stat = context.getFtpStatistics();

        // total directory created
        if (varName.equals(STAT_DIR_CREATE_COUNT)) {
            varVal = String.valueOf(stat.getTotalDirectoryCreated());
        }

        // total directory removed
        else if (varName.equals(STAT_DIR_DELETE_COUNT)) {
            varVal = String.valueOf(stat.getTotalDirectoryRemoved());
        }

        return varVal;
    }

    /**
     * Get statistical file variable value.
     */
    private static String getStatisticalFileVariableValue(FtpIoSession session,
            FtpServerContext context, String varName) {
        String varVal = null;
        FtpStatistics stat = context.getFtpStatistics();

        // total number of file upload
        if (varName.equals(STAT_FILE_UPLOAD_COUNT)) {
            varVal = String.valueOf(stat.getTotalUploadNumber());
        }

        // total bytes uploaded
        else if (varName.equals(STAT_FILE_UPLOAD_BYTES)) {
            varVal = String.valueOf(stat.getTotalUploadSize());
        }

        // total number of file download
        else if (varName.equals(STAT_FILE_DOWNLOAD_COUNT)) {
            varVal = String.valueOf(stat.getTotalDownloadNumber());
        }

        // total bytes downloaded
        else if (varName.equals(STAT_FILE_DOWNLOAD_BYTES)) {
            varVal = String.valueOf(stat.getTotalDownloadSize());
        }

        // total number of files deleted
        else if (varName.equals(STAT_FILE_DELETE_COUNT)) {
            varVal = String.valueOf(stat.getTotalDeleteNumber());
        }

        return varVal;
    }

    /**
     * Get statistical login variable value.
     */
    private static String getStatisticalLoginVariableValue(
            FtpIoSession session, FtpServerContext context, String varName) {
        String varVal = null;
        FtpStatistics stat = context.getFtpStatistics();

        // total login number
        if (varName.equals(STAT_LOGIN_TOTAL)) {
            varVal = String.valueOf(stat.getTotalLoginNumber());
        }

        // current login number
        else if (varName.equals(STAT_LOGIN_CURR)) {
            varVal = String.valueOf(stat.getCurrentLoginNumber());
        }

        // total anonymous login number
        else if (varName.equals(STAT_LOGIN_ANON_TOTAL)) {
            varVal = String.valueOf(stat.getTotalAnonymousLoginNumber());
        }

        // current anonymous login number
        else if (varName.equals(STAT_LOGIN_ANON_CURR)) {
            varVal = String.valueOf(stat.getCurrentAnonymousLoginNumber());
        }

        return varVal;
    }

    /**
     * Get statistical variable value.
     */
    private static String getStatisticalVariableValue(FtpIoSession session,
            FtpServerContext context, String varName) {

        String varVal = null;
        FtpStatistics stat = context.getFtpStatistics();

        // server start time
        if (varName.equals(STAT_START_TIME)) {
            varVal = DateUtils.getISO8601Date(stat.getStartTime().getTime());
        }

        // connection statistical variables
        else if (varName.startsWith("stat.con")) {
            varVal = getStatisticalConnectionVariableValue(session, context,
                    varName);
        }

        // login statistical variables
        else if (varName.startsWith("stat.login.")) {
            varVal = getStatisticalLoginVariableValue(session, context, varName);
        }

        // file statistical variable
        else if (varName.startsWith("stat.file")) {
            varVal = getStatisticalFileVariableValue(session, context, varName);
        }

        // directory statistical variable
        else if (varName.startsWith("stat.dir.")) {
            varVal = getStatisticalDirectoryVariableValue(session, context,
                    varName);
        }

        return varVal;
    }

    private LocalizedFtpReply(int code, String message) {
        super(code, message);
    }   
}
