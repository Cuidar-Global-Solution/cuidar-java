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

CREATE TABLE tb_usuario_pessoa_cuidado (
    cd_usuario NUMBER(10) NOT NULL,
    cd_pessoa_acompanhada NUMBER(10) NOT NULL,
    CONSTRAINT pk_tb_usuario_pessoa_cuidado PRIMARY KEY (cd_usuario, cd_pessoa_acompanhada),
    CONSTRAINT fk_tb_upc_usuario FOREIGN KEY (cd_usuario)
        REFERENCES tb_usuario (cd_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_tb_upc_pessoa FOREIGN KEY (cd_pessoa_acompanhada)
        REFERENCES tb_pessoa_acompanhada (cd_pessoa_acompanhada) ON DELETE CASCADE
);

CREATE INDEX ix_tb_upc_pessoa ON tb_usuario_pessoa_cuidado (cd_pessoa_acompanhada);
