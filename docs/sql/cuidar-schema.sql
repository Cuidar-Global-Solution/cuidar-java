CREATE TABLE tb_usuario (
    cd_usuario           NUMBER(10)    NOT NULL,
    nm_usuario           VARCHAR2(100) NOT NULL,
    ds_email             VARCHAR2(120) NOT NULL,
    ds_senha             VARCHAR2(100) NOT NULL,
    nr_telefone          VARCHAR2(20),
    tp_usuario           VARCHAR2(15)  NOT NULL,
    tp_plano             VARCHAR2(10)  NOT NULL,
    tp_canal_lembrete    VARCHAR2(10)  NOT NULL,
    tp_frequencia_resumo VARCHAR2(10)  NOT NULL,
    dt_cadastro          DATE          NOT NULL,

    CONSTRAINT pk_tb_usuario PRIMARY KEY (cd_usuario),
    CONSTRAINT uq_tb_usuario_email UNIQUE (ds_email),
    CONSTRAINT ck_tb_usuario_tipo
        CHECK (tp_usuario IN ('FAMILIAR', 'PROFISSIONAL', 'ACOMPANHADO')),
    CONSTRAINT ck_tb_usuario_plano
        CHECK (tp_plano IN ('GRATUITO', 'FAMILIA')),
    CONSTRAINT ck_tb_usuario_canal
        CHECK (tp_canal_lembrete IN ('CELULAR', 'EMAIL', 'AMBOS')),
    CONSTRAINT ck_tb_usuario_resumo
        CHECK (tp_frequencia_resumo IN ('SEMANAL', 'MENSAL', 'NENHUM'))
);


CREATE SEQUENCE sq_tb_usuario
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE tb_pessoa_acompanhada (
    cd_pessoa_acompanhada NUMBER(10)    NOT NULL,
    cd_usuario            NUMBER(10),
    nm_pessoa             VARCHAR2(100) NOT NULL,
    dt_nascimento         DATE          NOT NULL,
    ds_observacao         VARCHAR2(300),
    nr_tamanho_fonte      NUMBER(2)     NOT NULL,
    fl_alto_contraste     CHAR(1)       NOT NULL,

    CONSTRAINT pk_tb_pessoa_acompanhada
        PRIMARY KEY (cd_pessoa_acompanhada),
    CONSTRAINT uq_tb_pessoa_acomp_usuario
        UNIQUE (cd_usuario),
    CONSTRAINT fk_tb_pessoa_acomp_usuario
        FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT ck_tb_pessoa_acomp_fonte
        CHECK (nr_tamanho_fonte BETWEEN 12 AND 32),
    CONSTRAINT ck_tb_pessoa_acomp_contraste
        CHECK (fl_alto_contraste IN ('S', 'N'))
);

CREATE SEQUENCE sq_tb_pessoa_acompanhada
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE tb_area_cuidado (
    cd_area NUMBER(3)     NOT NULL,
    nm_area VARCHAR2(50)  NOT NULL,
    ds_area VARCHAR2(200),

    CONSTRAINT pk_tb_area_cuidado PRIMARY KEY (cd_area),
    CONSTRAINT uq_tb_area_cuidado_nome UNIQUE (nm_area)
);

CREATE SEQUENCE sq_tb_area_cuidado
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE tb_medicamento (
    cd_medicamento       NUMBER(10)    NOT NULL,
    cd_pessoa_acompanhada NUMBER(10)   NOT NULL,
    nm_medicamento       VARCHAR2(100) NOT NULL,
    ds_dose              VARCHAR2(50)  NOT NULL,
    ds_frequencia        VARCHAR2(80)  NOT NULL,
    dt_inicio            DATE          NOT NULL,
    dt_fim               DATE,

    CONSTRAINT pk_tb_medicamento PRIMARY KEY (cd_medicamento),
    CONSTRAINT fk_tb_medicamento_pessoa
        FOREIGN KEY (cd_pessoa_acompanhada)
        REFERENCES tb_pessoa_acompanhada (cd_pessoa_acompanhada),
    CONSTRAINT ck_tb_medicamento_datas CHECK (dt_fim >= dt_inicio)
);

CREATE SEQUENCE sq_tb_medicamento START WITH 1 INCREMENT BY 1;

CREATE TABLE tb_consulta (
    cd_consulta           NUMBER(10)    NOT NULL,
    cd_pessoa_acompanhada NUMBER(10)    NOT NULL,
    tp_consulta           VARCHAR2(10)  NOT NULL,
    ds_titulo             VARCHAR2(100) NOT NULL,
    ds_local              VARCHAR2(120),
    nm_acompanhante       VARCHAR2(100),
    dt_consulta           DATE          NOT NULL,
    ds_resultado          VARCHAR2(300),
    ds_orientacao         VARCHAR2(300),

    CONSTRAINT pk_tb_consulta PRIMARY KEY (cd_consulta),
    CONSTRAINT fk_tb_consulta_pessoa
        FOREIGN KEY (cd_pessoa_acompanhada)
        REFERENCES tb_pessoa_acompanhada (cd_pessoa_acompanhada),
    CONSTRAINT ck_tb_consulta_tipo CHECK (tp_consulta IN ('CONSULTA', 'EXAME'))
);

CREATE SEQUENCE sq_tb_consulta START WITH 1 INCREMENT BY 1;

CREATE TABLE tb_pessoa_area_cuidado (
    cd_pessoa_area        NUMBER(10) NOT NULL,
    cd_pessoa_acompanhada NUMBER(10) NOT NULL,
    cd_area               NUMBER(3)  NOT NULL,
    dt_inclusao           DATE       NOT NULL,

    CONSTRAINT pk_tb_pessoa_area_cuidado PRIMARY KEY (cd_pessoa_area),
    CONSTRAINT uq_tb_pessoa_area_par
        UNIQUE (cd_pessoa_acompanhada, cd_area),
    CONSTRAINT fk_tb_pessoa_area_pessoa
        FOREIGN KEY (cd_pessoa_acompanhada)
        REFERENCES tb_pessoa_acompanhada (cd_pessoa_acompanhada),
    CONSTRAINT fk_tb_pessoa_area_area
        FOREIGN KEY (cd_area) REFERENCES tb_area_cuidado (cd_area)
);

CREATE SEQUENCE sq_tb_pessoa_area_cuidado START WITH 1 INCREMENT BY 1;

CREATE TABLE tb_registro_cuidado (
    cd_registro           NUMBER(10)    NOT NULL,
    cd_pessoa_acompanhada NUMBER(10)    NOT NULL,
    cd_area               NUMBER(3)     NOT NULL,
    cd_usuario_criou      NUMBER(10)    NOT NULL,
    cd_usuario_realizou   NUMBER(10),
    cd_medicamento        NUMBER(10),
    cd_consulta           NUMBER(10),
    ds_titulo             VARCHAR2(100) NOT NULL,
    ds_detalhe            VARCHAR2(300),
    dt_prevista           DATE,
    dt_realizado          DATE,
    st_registro           VARCHAR2(20)  NOT NULL,
    vl_medida             NUMBER(8,2),
    vl_medida_2           NUMBER(8,2),
    ds_unidade            VARCHAR2(20),

    CONSTRAINT pk_tb_registro_cuidado PRIMARY KEY (cd_registro),
    CONSTRAINT fk_tb_reg_cuidado_pessoa
        FOREIGN KEY (cd_pessoa_acompanhada)
        REFERENCES tb_pessoa_acompanhada (cd_pessoa_acompanhada),
    CONSTRAINT fk_tb_reg_cuidado_area
        FOREIGN KEY (cd_area) REFERENCES tb_area_cuidado (cd_area),
    CONSTRAINT fk_tb_reg_cuidado_criou
        FOREIGN KEY (cd_usuario_criou) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_tb_reg_cuidado_realizou
        FOREIGN KEY (cd_usuario_realizou) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_tb_reg_cuidado_med
        FOREIGN KEY (cd_medicamento) REFERENCES tb_medicamento (cd_medicamento),
    CONSTRAINT fk_tb_reg_cuidado_consulta
        FOREIGN KEY (cd_consulta) REFERENCES tb_consulta (cd_consulta),
    CONSTRAINT ck_tb_reg_cuidado_status
        CHECK (st_registro IN (
            'PENDENTE', 'TOMADA', 'NAO_TOMADA', 'SEM_CONFIRMACAO', 'CONCLUIDO'
        )),
    CONSTRAINT ck_tb_reg_cuidado_datas
        CHECK (dt_prevista IS NOT NULL OR dt_realizado IS NOT NULL)
);

CREATE SEQUENCE sq_tb_registro_cuidado START WITH 1 INCREMENT BY 1;

SELECT * FROM TB_USUARIO;
