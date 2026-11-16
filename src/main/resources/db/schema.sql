CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(72) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    token_acesso VARCHAR(80),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_acesso VARCHAR(80);
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_token ON usuario (token_acesso);

CREATE TABLE IF NOT EXISTS categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS produto (
    id BIGSERIAL PRIMARY KEY,
    categoria_id BIGINT NOT NULL REFERENCES categoria (id),
    nome VARCHAR(150) NOT NULL,
    preco_atual NUMERIC(12, 2) NOT NULL,
    custo_atual NUMERIC(12, 2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_produto_preco CHECK (preco_atual >= 0)
);

CREATE TABLE IF NOT EXISTS pedido (
    id BIGSERIAL PRIMARY KEY,
    data_hora TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo_estorno VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS pedido_item (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    produto_id BIGINT NOT NULL REFERENCES produto (id),
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(12, 2) NOT NULL,
    custo_unitario NUMERIC(12, 2),
    CONSTRAINT ck_pedido_item_qtd CHECK (quantidade > 0),
    CONSTRAINT ck_pedido_item_preco CHECK (preco_unitario >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pedido_data ON pedido (data_hora);
CREATE INDEX IF NOT EXISTS idx_pedido_item_pedido ON pedido_item (pedido_id);

CREATE TABLE IF NOT EXISTS restaurante (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    admin_id BIGINT NOT NULL REFERENCES usuario (id),
    status VARCHAR(20) NOT NULL,
    inativado_pelo_admin BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS nome VARCHAR(120);
UPDATE usuario SET nome = split_part(email, '@', 1) WHERE nome IS NULL OR btrim(nome) = '';
ALTER TABLE usuario ALTER COLUMN nome SET NOT NULL;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS inativado_pelo_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS inativado_pelo_restaurante BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS comanda (
    id BIGSERIAL PRIMARY KEY,
    restaurante_id BIGINT NOT NULL REFERENCES restaurante (id),
    usuario_id BIGINT REFERENCES usuario (id),
    criada_em TIMESTAMPTZ NOT NULL,
    aberta BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE pedido ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS usuario_nome VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS comanda_id BIGINT REFERENCES comanda (id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS mesa INTEGER;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS nome_cliente VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS motivo_cancelamento VARCHAR(255);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS data_hora_estorno TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS data_hora_cancelamento TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS autor_estorno VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS autor_cancelamento VARCHAR(120);

CREATE TABLE IF NOT EXISTS pedido_alteracao (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    data_hora TIMESTAMPTZ NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    autor VARCHAR(120) NOT NULL
);

ALTER TABLE categoria ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);

CREATE INDEX IF NOT EXISTS idx_restaurante_admin ON restaurante (admin_id);
CREATE INDEX IF NOT EXISTS idx_usuario_restaurante ON usuario (restaurante_id);
CREATE INDEX IF NOT EXISTS idx_pedido_restaurante ON pedido (restaurante_id);
CREATE INDEX IF NOT EXISTS idx_pedido_usuario ON pedido (usuario_id);
CREATE INDEX IF NOT EXISTS idx_pedido_comanda ON pedido (comanda_id);
CREATE INDEX IF NOT EXISTS idx_comanda_restaurante ON comanda (restaurante_id);
