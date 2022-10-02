package org.example;

import javax.sql.RowSet;
import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataPanel extends JPanel {

    private List<JTextField> fields;
    public DataPanel(RowSet rowSet) throws SQLException {
        fields = new ArrayList<>();
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.gridwidth = 1;

        // pokaz pola tekstowe z kolumnami z bazy danych
        ResultSetMetaData resultSetMetaData = rowSet.getMetaData();
        for(int i=1 ; i<=resultSetMetaData.getColumnCount(); i++){
            gridBagConstraints.gridy = i -1;
            String columnName = resultSetMetaData.getColumnName(i);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            add(new JLabel(columnName), gridBagConstraints);

            //pola tekstowe z zartością
            int columnWidth = resultSetMetaData.getColumnDisplaySize(i);
            JTextField textField = new JTextField(columnWidth);
            //mozna edytowac tylko pola tekstowe
            if(!resultSetMetaData.getColumnClassName(i).equals("java.lang.String"))
                textField.setEditable(false);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            add(textField, gridBagConstraints);
            fields.add(textField);
        }

    }

    public void showRow(ResultSet rs){
        try{
            if(rs == null) return;
            for(int i=1; i<=fields.size(); i++){
                String field = rs==null ? "" : rs.getString(i);
                JTextField textField = fields.get(i-1);
                textField.setText(field);

            }
        } catch (SQLException e) {
            for(Throwable t: e)
                t.printStackTrace();
        }
    }

    public void setRow(RowSet rs) throws SQLException {
        for(int i=1; i<= fields.size(); i++){
            String field = rs.getString(i);
            JTextField tb = fields.get(i-1);
            if(!field.equals(tb.getText()))
                rs.updateString(i, tb.getText());
        }
        rs.updateRow();
    }
}
