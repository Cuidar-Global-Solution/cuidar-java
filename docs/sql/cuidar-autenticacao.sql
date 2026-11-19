-- Aplicar depois de cuidar-schema.sql. Revisar duplicidades de e-mail antes da execução.
CREATE UNIQUE INDEX ux_tb_usuario_email_ci ON tb_usuario (LOWER(TRIM(ds_email)));

CREATE TABLE tb_sessao (
    ds_token_hash CHAR(64) NOT NULL,
    cd_usuario NUMBER(10) NOT NULL,
    dt_expiracao TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tb_sessao PRIMARY KEY (ds_token_hash),
    CONSTRAINT fk_tb_sessao_usuario FOREIGN KEY (cd_usuario)
        REFERENCES tb_usuario (cd_usuario) ON DELETE CASCADE
);

CREATE INDEX ix_tb_sessao_usuario ON tb_sessao (cd_usuario);
CREATE INDEX ix_tb_sessao_expiracao ON tb_sessao (dt_expiracao);

CREATE TABLE tb_vinculo (
    cd_vinculo NUMBER(10) NOT NULL,
    cd_usuario NUMBER(10) NOT NULL,
    cd_pessoa_acompanhada NUMBER(10) NOT NULL,
    tp_relacao VARCHAR2(30) NOT NULL,
    tp_permissao VARCHAR2(12) NOT NULL,
    st_autorizacao VARCHAR2(10) NOT NULL,
    dt_vinculo DATE NOT NULL,
    CONSTRAINT pk_tb_vinculo PRIMARY KEY (cd_vinculo),
    CONSTRAINT ck_tb_vinculo_permissao CHECK (tp_permissao IN ('VISUALIZAR', 'REGISTRAR')),
    CONSTRAINT ck_tb_vinculo_autorizacao CHECK (st_autorizacao IN ('PENDENTE', 'ATIVO', 'REVOGADO')),
    CONSTRAINT fk_tb_vinculo_usuario FOREIGN KEY (cd_usuario)
        REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_tb_vinculo_pessoa FOREIGN KEY (cd_pessoa_acompanhada)
        REFERENCES tb_pessoa_acompanhada (cd_pessoa_acompanhada)
);

CREATE SEQUENCE sq_tb_vinculo START WITH 1 INCREMENT BY 1;

CREATE INDEX ix_tb_vinculo_pessoa ON tb_vinculo (cd_pessoa_acompanhada);
