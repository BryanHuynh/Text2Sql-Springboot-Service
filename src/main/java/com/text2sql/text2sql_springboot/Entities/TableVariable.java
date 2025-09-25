package com.text2sql.text2sql_springboot.Entities;

import jakarta.persistence.*;

import java.util.UUID;


@Entity
@Table(name = "TABLE_VARIABLES", schema = "public")
public class TableVariable {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "variable_name", length = 255)
    private String variableName;

    @Column(name = "variable_type", length = 255)
    private String variableType;

    @Column(name = "pk_flag")
    private boolean pkFlag;

    @Column(name = "fk_flag")
    private boolean fkFlag;

    @Column(name = "variable_order")
    private int order;

    @ManyToOne
    @JoinColumn(name = "fk_ref_id", foreignKey = @ForeignKey(name = "fk_table_variable"))
    private TableVariable fkRef;

    @ManyToOne
    @JoinColumn(name = "usertable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usertable"))
    private UserTable userTable;

    public TableVariable(Builder builder) {
        this.variableName = builder.variableName;
        this.variableType = builder.variableType;
        this.pkFlag = builder.pkFlag;
        this.fkFlag = builder.fkFlag;
        this.fkRef = builder.fkRef;
        this.userTable = builder.userTable;
        this.order = builder.order;
    }

    public TableVariable() {
    }

    ;


    public UserTable getUserTable() {
        return userTable;
    }

    public void setUserTable(UserTable userTable) {
        this.userTable = userTable;
    }

    public static class Builder {
        private String variableName;
        private String variableType;
        private boolean pkFlag;
        private boolean fkFlag;
        private TableVariable fkRef;
        private UserTable userTable;
        private int order;

        public Builder variableName(String variableName) {
            this.variableName = variableName;
            return this;
        }

        public Builder variableType(String variableType) {
            this.variableType = variableType;
            return this;
        }

        public Builder pkFlag(boolean pkFlag) {
            this.pkFlag = pkFlag;
            return this;
        }

        public Builder fkFlag(boolean fkFlag) {
            this.fkFlag = fkFlag;
            return this;
        }

        public Builder fkRef(TableVariable fkRef) {
            this.fkRef = fkRef;
            return this;
        }

        public Builder userTable(UserTable userTable) {
            this.userTable = userTable;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public TableVariable build() {
            return new TableVariable(this);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableType() {
        return variableType;
    }

    public int getOrder() {
        return order;
    }

    public boolean isPkFlag() {
        return pkFlag;
    }

    public boolean isFkFlag() {
        return fkFlag;
    }

    public TableVariable getFkRef() {
        if (fkRef == null) return null;
        return fkRef;
    }

    public void setFkRef(TableVariable fkRef) {
        this.fkRef = fkRef;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    public void setPkFlag(boolean pkFlag) {
        this.pkFlag = pkFlag;
    }

    public void setFkFlag(boolean fkFlag) {
        this.fkFlag = fkFlag;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
