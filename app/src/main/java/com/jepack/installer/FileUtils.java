package com.jepack.installer;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

/**
 * 文件处理
 * Created by haihai.zhang on 2017/2/17.
 */

public class FileUtils {

    public static void copyRawTo(Context context, int rawId, String dst, boolean deleteExists) throws IOException {
        File file = new File(dst);
        new File(file.getParent()).mkdirs();
        if(deleteExists && file.exists()){
            file.delete();
        }
        writeAll(context.getResources().openRawResource(rawId), file);

    }

    public static boolean fileIsExpire(String strLocalFile , String strRemoteMD5 ){
        boolean bRet = true;
        if ( fileIsExists(strLocalFile)){

            String strMD5 = MD5( strLocalFile );
            if ( strMD5.compareToIgnoreCase(strRemoteMD5) == 0 ){
                bRet = false;
            }

        }

        return bRet;
    }

    public static String toHexString(byte[] b) {
        char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static String MD5(String strFilePath){

        if ( strFilePath == null || strFilePath.length() <= 0 ){
            return "";
        }
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5;
        try{
            fis = new FileInputStream(strFilePath);
            md5 = MessageDigest.getInstance("MD5");
            while((numRead=fis.read(buffer)) > 0) {
                md5.update(buffer,0,numRead);
            }
            fis.close();
            return toHexString(md5.digest());
        } catch (Exception e) {
            System.out.println("error");
            return "";
        }
    }
    public static String MD5String(String strContent){

        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(strContent.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();

    }

    public static String readRaw(Context context, int rawId) throws IOException {
        BufferedSource bufferedSource = Okio.buffer(Okio.source(context.getResources().openRawResource(rawId)));
        return bufferedSource.readUtf8();

    }

    public static void writeStreamTo(String filePath, InputStream inputStream) throws IOException {
        Source source =  Okio.source(inputStream);
        BufferedSink sink = Okio.buffer(Okio.sink(new File(filePath)));
        sink.writeAll(source);
        sink.flush();
        sink.close();
        source.close();
    }

    public static void writeStringTo(String filePath, String str) throws IOException {
        BufferedSink sink = Okio.buffer(Okio.sink(new File(filePath)));
        sink.write(str.getBytes());
        sink.flush();
        sink.close();
    }

    public static void writeBytesTo(String filePath, byte[] bytes) {
        File file = new File(filePath);
        BufferedSink sink = null;
        try {
            sink = Okio.buffer(Okio.sink(file));
            sink.write(bytes);
            sink.flush();
            sink.close();
        } catch (FileNotFoundException e) {
            LogUtil.e(e);
        } catch (IOException e) {
            LogUtil.e(e);
        }finally {
            if(sink != null) {
                try {
                    sink.close();
                } catch (IOException e) {
                    LogUtil.e(e);
                }
            }
        }

    }

    public static byte[] readFileBytes(String path) {
        Source source = null;
        BufferedSource bufferedSource = null;
        try {
            source = Okio.source(new File(path));
            bufferedSource = Okio.buffer(source);
            return bufferedSource.readByteArray();
        } catch (FileNotFoundException e) {
            LogUtil.e(e);
        } catch (IOException e) {
            LogUtil.e(e);
        }finally {
            try {
                if(source != null){
                    source.close();
                }

                if(bufferedSource != null){
                    bufferedSource.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static String readFile(String path) throws IOException {
        Source source = Okio.source(new File(path));
        BufferedSource bufferedSource = Okio.buffer(source);
        return bufferedSource.readUtf8();
    }

    public static void appendStrToFile(String str, String path) throws IOException {
        if(str == null) return;
        BufferedSink sink = Okio.buffer(Okio.appendingSink(new File(path)));
        sink.writeUtf8(str);
        sink.flush();
        sink.close();
    }



    //fyy 2017-02-21 文件是否存在
    public static  boolean fileIsExists( String strFilePath ){
        try{
            File f=new File(strFilePath);
            if(f.isFile() && f.exists()  ){
                return true;
            }

        }catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return false;
    }
    //fyy 2017-2-21 文件扩展名
    public static String getExtName(String s) {
        String prefix = "";
        try {
            File f = new File(s);
            String fileName = f.getName();
            prefix = fileName.substring(fileName.lastIndexOf("") + 1);
        }
        catch (Exception e){

        }
        return prefix;
    }

    //2017-2-21 文件扩展名
    public static String getExtNameNoFile(String strPath){
        int nPos = strPath.lastIndexOf("");
        String strPrefix = "";
        if ( nPos > 0 ){
            strPrefix = strPath.substring( nPos + 1 );
        }//

        return strPrefix;
    }

    public static boolean emptyFolderExclude(File parent, File exFile){
        if(parent.isDirectory()) {
            File[] files = parent.listFiles();
            if(files != null && files.length > 0) {
                for (File file : files) {
                    if (exFile == null || file.getAbsoluteFile().equals(exFile.getAbsoluteFile())) {
                        return true;
                    } else {
                        if (file.exists() && file.isFile()) {
                            return deleteFile(file);
                        } else {
                            return !file.exists() || emptyFolderExclude(file, exFile);
                        }
                    }
                }
            }
            return true;
        }else{
            return deleteFile(parent);
        }
    }

    public static boolean deleteFolder(String folder){
        File folderF = new File(folder);
        boolean ret = emptyFolderExclude(folderF, null);
        if(ret) {
            ret = folderF.delete();
            return ret;
        }else{
            return  ret;
        }
    }

    public static  boolean deleteFile( String strFilePath ){
        File f=new File(strFilePath);
        return deleteFile(f);
    }

    //fyy 2017-03-01 删除文件
    public static  boolean deleteFile( File f ){
        try{
            boolean bRet = false;
            if(f.exists() && f.isFile() ){
                bRet = f.delete();
            }
            return bRet;
        }catch (Exception e) {
            // TODO: handle exception
            return false;
        }

    }
    //fyy 2017-03-02 获取文件父路径
    public static String getParentPath(String strFilePath ){
        String strPath = "";
        try{
            File f=new File(strFilePath);
            if(f.exists()){
                strPath = f.getParent();
            }
            return strPath;
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return strPath;
    }

    //fyy 2017-12-07 获取文件父路径
    public static String getParentPath2(String strFilePath ){
        String strPath = "";
        try{
            int nPos = strFilePath.lastIndexOf("/");
            if ( nPos > -1 ){
                strPath = strFilePath.substring( 0 , nPos );
            }
            return strPath;
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return strPath;
    }


    //fyy 2017-03-02 获取文件名
    public static String getFileName(String strFilePath ){
        try{
            String strPath = "";
            File f=new File(strFilePath);
            if(f.exists()){
                strPath = f.getName();
            }
            return strPath;
        }catch (Exception e) {
            // TODO: handle exception
            return "";
        }
    }

    //fyy 2017-03-02 获取文件名
    public static String getFileName2(String strFilePath ){
        try{
            String strPath = "";
            int nPos = strFilePath.lastIndexOf("/");
            if ( nPos > -1 ){
                strPath = strFilePath.substring( nPos + 1  );
            }
            return strPath;
        }catch (Exception e) {
            // TODO: handle exception
            return "";
        }
    }

    /**
     * zhanghaihai 2017-3-15
     * @param zipFile
     * @param folderPath
     * @throws ZipException
     * @throws IOException
     */
    public static void upZipFile(File zipFile, String folderPath) throws IOException {
        File desDir = new File(folderPath);
        if (!new File(folderPath).exists()) desDir.mkdirs();

        ZipFile zf = new ZipFile(zipFile);
        for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
            ZipEntry entry = ((ZipEntry) entries.nextElement());


            // str = new String(str.getBytes("8859_1"), "GB2312");
            File desFile = new File(folderPath, entry.getName());

            //删除旧文件
            if (!desFile.isDirectory()) {
                desFile.delete();
            }

            //是文件夹创建文件夹后返回
            if(entry.isDirectory()){
                desFile.mkdirs();
                continue;
            }else {
                //非文件夹，判断父文件夹是否存在，不存在则创建
                File fileParentDir = desFile.getParentFile();
                fileParentDir.mkdirs();
            }

            //写入文件
            InputStream in = zf.getInputStream(entry);
            writeAll(in, desFile);

        }
    }

    public static long writeAll(InputStream in, File desFile) throws IOException {
        Source source = Okio.source(in);
        BufferedSink sink = Okio.buffer(Okio.sink(desFile));
        long length = sink.writeAll(source);
        sink.flush();
        sink.close();
        source.close();
        return length;
    }

    public static boolean isDirecotry(String strPath){
        boolean bRet = false;
        try{
            File file = new File(strPath);
            if ( file.exists() && file.isDirectory() ){
                bRet = true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return bRet;
    }

    public static boolean makeDirs(String strPath){
        boolean bRet = false;
        try{
            File file = new File(strPath);
            if ( file.exists() == false ){
                bRet = file.mkdirs();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return bRet;
    }
}
