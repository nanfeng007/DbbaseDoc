package com.tlyong1992;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.rtf.RtfWriter2;

/**
 * 数据字典生成器 Mysql
 *
 * @author tlyong1992
 */
public class MysqlDoc {

    /**
     * 键类型字典 key:String value:String
     */
    private static Map<String, String> keyType = new HashMap<>();

    /**
     * 用户名
     */
    private static String username;

    /**
     * 密码
     */
    private static String password;

    /**
     * 目标数据库名
     */
    private static String database;

    /**
     * 链接url
     */
    private static String url;

    /**
     * 导出文档目录
     */
    private static String outputFile;

    // 初始化jdbc
    static {
        try {
            keyType.put("PRI", "主键");
            keyType.put("UNI", "唯一键");
            Class.forName("com.mysql.jdbc.Driver");

            Properties prop = new Properties();

            try {
                prop.load(MysqlDoc.class.getClassLoader().getResourceAsStream("config.properties"));
                username = prop.getProperty("jdbc_user");
                password = prop.getProperty("jdbc_password");
                url = prop.getProperty("url");
                database = prop.getProperty("database");
                outputFile = prop.getProperty("outputPath") + database + "-"
                        + new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()) + ".doc";
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询所有表的sql语句
     */
    private static String sql_get_all_tables = "select table_name,TABLE_COMMENT from INFORMATION_SCHEMA.tables where TABLE_SCHEMA='"
            + database + "' and TABLE_TYPE='BASE TABLE'";

    /**
     * 查询所有字段的sql语句 CHARACTER_MAXIMUM_LENGTH以字符为单位的最大长度，适于二进制数据、字符数据，或者文本和图像数据。
     * CHARACTER_OCTET_LENGTH以字节为单位的最大长度，适于二进制数据、字符数据，或者文本和图像数据。
     */
    private static String sql_get_all_columns = "select column_name,data_type,character_octet_length,COLUMN_COMMENT,is_nullable,COLUMN_key from information_schema.`COLUMNS` where TABLE_NAME='{table_name}' and TABLE_SCHEMA='"
            + database + "'";

    public static void main(String[] args) throws Exception {
        // 初始化word文档
        Document document = new Document(PageSize.A4);
        document.addTitle(database);
        RtfWriter2.getInstance(document, new FileOutputStream(outputFile));
        document.open();
        // 查询开始
        Connection conn = getConnection();
        // 获取所有表
        List tables = getDataBySQL(sql_get_all_tables, conn);
        int i = 1;
        for (Iterator iterator = tables.iterator(); iterator.hasNext();) {
            String[] arr = (String[]) iterator.next();
            // 循环获取字段信息
            System.out.print(i + ".正在处理数据表-----------" + arr[0]);
            addTableMetaData(document, arr, i);
            List columns = getDataBySQL(sql_get_all_columns.replace("{table_name}", arr[0]), conn);
            addTableDetail(document, columns);
            addBlank(document);
            System.out.println("...done");
            i++;
        }
        document.close();
        conn.close();
    }

    /**
     * 添加一个空行
     *
     * @param document
     * @throws Exception
     */
    public static void addBlank(Document document) throws Exception {
        Paragraph ph = new Paragraph("");
        ph.setAlignment(Paragraph.ALIGN_LEFT);
        document.add(ph);
    }

    /**
     * 添加包含字段详细信息的表格
     *
     * @param document
     * @param columns
     * @throws Exception
     */
    public static void addTableDetail(Document document, List columns) throws Exception {
        Table table = new Table(6);
        // 表格 宽度100%
        table.setWidth(100f);
        table.setBorderWidth(1);
        table.setBorderColor(Color.BLACK);
        table.setPadding(0);
        table.setSpacing(0);
        // 单元格
        Cell cell1 = new Cell("序号");
        cell1.setHeader(true);
        // 单元格
        Cell cell2 = new Cell("列名");
        cell2.setHeader(true);
        // 单元格
        Cell cell3 = new Cell("类型");
        cell3.setHeader(true);
        // 单元格
        Cell cell4 = new Cell("长度");
        cell4.setHeader(true);
        // 单元格
        Cell cell5 = new Cell("键");
        cell5.setHeader(true);
        // 单元格
        Cell cell6 = new Cell("说明");
        cell6.setHeader(true);
        // 设置表头格式
        table.setWidths(new float[] { 8f, 30f, 15f, 8f, 10f, 29f });
        cell1.setHorizontalAlignment(Cell.ALIGN_CENTER);
        cell1.setBackgroundColor(StyleConfig.TABLE_HEAD_COLOR);
        cell2.setHorizontalAlignment(Cell.ALIGN_CENTER);
        cell2.setBackgroundColor(StyleConfig.TABLE_HEAD_COLOR);
        cell3.setHorizontalAlignment(Cell.ALIGN_CENTER);
        cell3.setBackgroundColor(StyleConfig.TABLE_HEAD_COLOR);
        cell4.setHorizontalAlignment(Cell.ALIGN_CENTER);
        cell4.setBackgroundColor(StyleConfig.TABLE_HEAD_COLOR);
        cell5.setHorizontalAlignment(Cell.ALIGN_CENTER);
        cell5.setBackgroundColor(StyleConfig.TABLE_HEAD_COLOR);
        cell6.setHorizontalAlignment(Cell.ALIGN_CENTER);
        cell6.setBackgroundColor(StyleConfig.TABLE_HEAD_COLOR);
        table.addCell(cell1);
        table.addCell(cell2);
        table.addCell(cell3);
        table.addCell(cell4);
        table.addCell(cell5);
        table.addCell(cell6);
        table.endHeaders();// 表头结束
        int x = 1;
        for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
            String[] arr2 = (String[]) iterator.next();
            Cell c1 = new Cell(x + "");
            Cell c2 = new Cell(arr2[0]);
            Cell c3 = new Cell(arr2[1]);
            Cell c4 = new Cell(arr2[2]);

            String key = keyType.get(arr2[5]);
            if (key == null) {
                key = "";
            }
            Cell c5 = new Cell(key);
            Cell c6 = new Cell(arr2[3]);
            c1.setHorizontalAlignment(Cell.ALIGN_CENTER);
            c2.setHorizontalAlignment(Cell.ALIGN_CENTER);
            c3.setHorizontalAlignment(Cell.ALIGN_CENTER);
            c4.setHorizontalAlignment(Cell.ALIGN_CENTER);
            c5.setHorizontalAlignment(Cell.ALIGN_CENTER);
            c6.setHorizontalAlignment(Cell.ALIGN_CENTER);
            table.addCell(c1);
            table.addCell(c2);
            table.addCell(c3);
            table.addCell(c4);
            table.addCell(c5);
            table.addCell(c6);
            x++;
        }
        document.add(table);
    }

    /**
     * 增加表概要信息
     *
     * @param dcument
     * @param arr
     * @param i
     * @throws Exception
     */
    public static void addTableMetaData(Document dcument, String[] arr, int i) throws Exception {
        Paragraph ph = new Paragraph(i + ". 表名: " + arr[0] + "        说明: " + (arr[1] == null ? "" : arr[1]));
        ph.setAlignment(Paragraph.ALIGN_LEFT);
        dcument.add(ph);
    }

    /**
     * 把SQL语句查询出列表
     *
     * @param sql
     * @param conn
     * @return
     */
    public static List getDataBySQL(String sql, Connection conn) {
        Statement stmt = null;
        ResultSet rs = null;
        List list = new ArrayList();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String[] arr = new String[rs.getMetaData().getColumnCount()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = rs.getString(i + 1);
                }
                list.add(arr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 获取数据库连接
     *
     * @return
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}