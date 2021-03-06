package tools;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.MyUrl;
import src.FileCharsetDetector;

public class WrongPathChecker {

  /**
   * 判断include标签中的路径是否有效.
   * 
   * @param rootPath
   *          项目根路径
   * @param strPath
   *          引用路径
   * @return 返回true或者false
   */
  public boolean isValidIncludePath(final String rootPath,
      final String strPath) {
    String include;
    Pattern pattern = Pattern.compile("/$");
    Matcher matcher = pattern.matcher(rootPath);
    if (matcher.find()) {
      include = "_include/";
    } else {
      include = "/_include/";
    }
    File folder = new File(rootPath + include + strPath);
    if (folder.exists()) {
      return true;
    }
    return false;
  }

  /**
   * 判断内部引用路径是否有效.
   * 
   * @param rootPath
   *          项目根路径
   * @param strPath
   *          引用路径
   * @return 返回true或者false
   */
  public boolean isValidIntercalPath(final String rootPath, final String strPath) {
    File intercalFile = new File(rootPath + "\\" + strPath);
    if (intercalFile.exists()) {
      return true;
    }
    return false;
  }

  /**
   * 在文件中搜索引用超链接中的错误路径.
   * 
   * @param file
   *          需要检测的文件
   * @param rootPath
   *          项目根路径
   */
  public List<MyUrl> searchWrongIntercalPath(final File file, final String rootPath) {
    List<MyUrl> wrongInternalPathList = new ArrayList<MyUrl>();
    // 检索引入超链接[超链接文本](超链接地址 "悬浮显示")的正则
    String intercalRex = "\\[[^\\(\\)]*\\]\\([^\\(\\)]+\\)";
    Pattern intercalPattern = Pattern.compile(intercalRex);
    // 检查是否为外部网址链接的正则
    // String httpRex = "(http://|https://)(([a-zA-z0-9\\?\\!\\&\\*\\#\\=\\/-]){1,}\\.){1,}"
    //    + "([a-zA-z0-9\\?\\!\\&\\*\\#\\=\\/-][^\\[\\]]。，\"\\(\\)\\{\\}){1,}";
    String httpRex = "(http://|https://)[^\\(\\)\\[\\]\\{\\}\\<\\>\\s，。；：\"]{1,}";
    Pattern httpPattern = Pattern.compile(httpRex);
    // 检测是否为锚点链接[文字](#锚点位置)的正则
    String anchorRex = "^\\#";
    Pattern anchorPattern = Pattern.compile(anchorRex);
    LineNumberReader lineReader = null;
    try {
      // 读取文件时指定字符编码
      lineReader = new LineNumberReader(new BufferedReader(
          new InputStreamReader(new FileInputStream(file), "UTF-8")));
      String readLine = null;

      // 读取文件内容
      while ((readLine = lineReader.readLine()) != null) {
        Matcher intercalMatcher = intercalPattern.matcher(readLine);
        while (intercalMatcher.find()) { // 假如这一行存在可以匹配引入超链接正则表达式的字符串
          // 将路径字符串从引入超链接的字符串中切割出来
          String intercalPath = intercalMatcher.group(0)
              .replaceAll("\\[[^\\(\\)]*\\]\\(", "").replaceAll("\\)", "")
              .split("\\s")[0];
          // 检测路径是否为内部路径，是否为锚点链接，以及路径是否正确
          Matcher httpMatcher = httpPattern.matcher(intercalPath);
          Matcher anchorMatcher = anchorPattern.matcher(intercalPath);
          if (!httpMatcher.find() && !anchorMatcher.find()
              && !isValidIntercalPath(rootPath, intercalPath.trim())) {
            MyUrl myUrl = new MyUrl();
            myUrl.setFile(file.getParent() + "\\" + file.getName());
            myUrl.setUrl(intercalPath);
            // 若路径不可用，将这个网址所属的文件名和网址字符串插入到错误路径list
            wrongInternalPathList.add(myUrl);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      // 关闭流
      close(lineReader);
    }
    return wrongInternalPathList;
  }

  /**
   * 关闭流.
   * 
   * @param able
   *          需要关闭的流
   */
  private void close(final Closeable able) {
    if (able != null) {
      try {
        able.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
