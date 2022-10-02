package org.example;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

public class ViewDB extends JFrame {

    private JComboBox<String> tableNames;
    private Properties properties;
    private CachedRowSet crs;
    private  JScrollPane scrollPane;
    private  DataPanel dataPanel;

    public ViewDB(){
        tableNames = new JComboBox<String>();
        try{
            readDatabaseProperties();
            Connection connection = getConnection();
            DatabaseMetaData meta = connection.getMetaData();
            try(ResultSet resultSet = meta.getTables("JDBCtest", null, null,
                    new String[]{"TABLE"})){
                while (resultSet.next())
                   tableNames.addItem(resultSet.getString(3));
            }

            tableNames.addActionListener(e -> showTable( (String) tableNames.getSelectedItem(), connection));

            add(tableNames, BorderLayout.NORTH);

            // przyciski
            JPanel buttonPanel = new JPanel();
            add(buttonPanel, BorderLayout.SOUTH);
            JButton previousButton = new JButton("Poprzedni");
            previousButton.addActionListener(e-> showPreviousRow());
            JButton nextButton = new JButton("Następny");
            nextButton.addActionListener(e -> showNextRow());
            JButton deleteButton = new JButton("Usuń");
            deleteButton.addActionListener(e -> deleteRow(connection));
            JButton saveButton = new JButton("Zapisz");
            saveButton.addActionListener(e -> saveChanges(connection));

            // dodaje przyciski
            buttonPanel.add(previousButton);
            buttonPanel.add(nextButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(saveButton);


            //zamknij polaczenie przy zamknieciu okna
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try{
                        if(connection != null) connection.close();
                    } catch (SQLException ex) {
                        for(Throwable t: ex)
                            t.printStackTrace();
                    }
                }
            });

            if(tableNames.getItemCount() > 0)
                showTable(tableNames.getItemAt(0), connection);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            for(Throwable t: e)
                t.printStackTrace();
        }

    }

    private void showNextRow(){
        try{
            if(crs == null || crs.isLast())
                return;
            crs.next();
            dataPanel.showRow(crs);
        } catch (SQLException e) {
            for(Throwable t: e)
                t.printStackTrace();
        }
    }

    private void  showPreviousRow(){
        try{
            if(crs == null || crs.isFirst())
                return;
            crs.previous();
            dataPanel.showRow(crs);
        } catch (SQLException e) {
            for(Throwable t: e)
                t.printStackTrace();
        }
    }

    private void saveChanges(Connection connection){
        if(crs == null) return;
        new SwingWorker<Void, Void>(){
            public Void doInBackground() throws SQLException {
                dataPanel.setRow(crs);
                crs.acceptChanges(connection);
                return  null;
            }
        }.execute();
    }

    private void deleteRow(Connection connection){
        if(crs == null) return;
        new SwingWorker<Void, Void>(){

            @Override
            protected Void doInBackground() throws SQLException {
                crs.deleteRow();
                crs.acceptChanges(connection);
                if(crs.isAfterLast())
                    if(!crs.last()) crs = null;
                return null;
            }
            protected void done(){
                dataPanel.showRow(crs);
            }
        }.execute();
    }

    private void showTable(String table, Connection connection){
        try(Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT  * FROM "+ table))
        {
            //Skopiowanie wynikow do pamieci podrecznej
            RowSetFactory factory = RowSetProvider.newFactory();
            crs = factory.createCachedRowSet();
            crs.setTableName(table);
            crs.populate(resultSet);

            if(scrollPane != null) remove(scrollPane);

            dataPanel= new DataPanel(crs);
            scrollPane = new JScrollPane(dataPanel);
            add(scrollPane, BorderLayout.CENTER);
            pack();
            showNextRow();

        } catch (SQLException e) {
            for(Throwable t: e)
                t.printStackTrace();
        }
    }

    private void readDatabaseProperties() throws IOException {
        properties = new Properties();
        try(InputStream input = Files.newInputStream(Paths.get("database.properties"))){
            properties.load(input);
        }

        String drivers = properties.getProperty("jdbc.drivers");
        if(drivers !=null) System.setProperty("jdbc.drivers", drivers);
    }

    private Connection getConnection() throws SQLException {
        String url = properties.getProperty("jdbc.url");
        String username = properties.getProperty("jdbc.username");
        String password = properties.getProperty("jdbc.password");

        return DriverManager.getConnection(url, username, password);
    }
}
