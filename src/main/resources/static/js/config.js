/**
 * Configuração central do sistema
 * Centraliza URLs, endpoints e constantes para facilitar manutenção
 */

// ENDPOINTS DA API REST

export const API = {
    // Bloqueio
    BLOQUEIO_STATUS: '/bloqueio/status',

    // Conselheiros
    CONSELHEIROS_POR_GESTAO: '/api/conselheiros/conselheiros-por-gestao',

    // Regras
    REGRAS_POR_DATA: '/api/regras/regras-por-data',
    REGRAS_POR_RESOLUCAO: '/api/regras/resolucao',

    // Jetons
    JETONS_ATIVIDADES: (pessoa, gestao, mes, ano) =>
        `/api/jetons/atividades/conselheiro/${pessoa}/gestao/${gestao}/mes/${mes}/ano/${ano}`,
    JETONS_RELATORIO_CONSELHEIRO: (pessoa, gestao, mes, ano) =>
        `/api/jetons/relatorio-conselheiro/${pessoa}/gestao/${gestao}/mes/${mes}/ano/${ano}`,
    JETONS_RELATORIO_EXPORT: (gestao, mes, ano, formato) =>
        `/jeton/relatorio?idGestao=${gestao}&mes=${mes}&ano=${ano}&formato=${formato}`,

    // Comprovantes
    COMPROVANTE_DOWNLOAD: (id) => `/comprovantes/download/${id}`,
};

// ROTAS DA APLICAÇÃO (MVC)

export const ROUTES = {
    // Atividades
    ATIVIDADE_VALIDAR: (id) => `/atividades/validar/${id}`,
    ATIVIDADE_DESVALIDAR: (id) => `/atividades/desvalidar/${id}`,
    ATIVIDADE_EXCLUIR: (id) => `/atividades/excluir/${id}`,
    ATIVIDADE_EDITAR: (id) => `/atividades/editar/${id}`,

    // Conselheiros
    CONSELHEIRO_EXCLUIR: (id) => `/conselheiros/excluir/${id}`,
    CONSELHEIRO_EDITAR: (id) => `/conselheiros/editar/${id}`,

    // Gestões
    GESTAO_EXCLUIR: (id) => `/gestoes/excluir/${id}`,
    GESTAO_EDITAR: (id) => `/gestoes/editar/${id}`,
    GESTAO_VINCULAR: (id) => `/gestao-conselheiros/vincular/${id}`,

    // Vínculos
    VINCULO_ALTERNAR_STATUS: (gestao, pessoa) =>
        `/gestao-conselheiros/alternar-status/${gestao}/${pessoa}`,
    VINCULO_EXCLUIR: (gestao, pessoa) =>
        `/gestao-conselheiros/excluir/${gestao}/${pessoa}`,

    // Portarias
    PORTARIA_EXCLUIR: (id) => `/portarias/excluir/${id}`,
    PORTARIA_REVOGAR: (id) => `/portarias/revogar/${id}`,
    PORTARIA_RESTAURAR: (id) => `/portarias/restaurar/${id}`,
    PORTARIA_EDITAR: (id) => `/portarias/editar/${id}`,

    // Regras
    REGRA_EXCLUIR: (id) => `/regras/excluir/${id}`,
    REGRA_REVOGAR: (id) => `/regras/revogar/${id}`,
    REGRA_RESTAURAR: (id) => `/regras/restaurar/${id}`,
    REGRA_EDITAR: (id) => `/regras/editar/${id}`,

    // Regras Conjuntas
    REGRA_CONJUNTA_EXCLUIR: (id) => `/regras-conjuntas/excluir/${id}`,
    REGRA_CONJUNTA_EDITAR: (id) => `/regras-conjuntas/editar/${id}`,

    // Resoluções
    RESOLUCAO_EXCLUIR: (id) => `/resolucoes/excluir/${id}`,
    RESOLUCAO_REVOGAR: (id) => `/resolucoes/revogar/${id}`,
    RESOLUCAO_RESTAURAR: (id) => `/resolucoes/restaurar/${id}`,
    RESOLUCAO_EDITAR: (id) => `/resolucoes/editar/${id}`,

    // Usuários
    USUARIO_EXCLUIR: (id) => `/usuarios/excluir/${id}`,
    USUARIO_EDITAR: (id) => `/usuarios/editar/${id}`,

    // Conselheiro (portal)
    CONSELHEIRO_ATIVIDADE_EXCLUIR: (id) => `/conselheiro/atividades/excluir/${id}`,
    CONSELHEIRO_ATIVIDADE_EDITAR: (id) => `/conselheiro/atividades/editar/${id}`,

    // Tipos de Anexo
    TIPO_ANEXO_EXCLUIR: (id) => `/tipos-anexo/excluir/${id}`,
    TIPO_ANEXO_EDITAR: (id) => `/tipos-anexo/editar/${id}`,
};

// CONSTANTES GERAIS

export const CONSTANTS = {
    // Paginação
    DEFAULT_PAGE_SIZE: 10,
    PAGE_SIZES: [10, 20, 50],

    // Datas
    DATE_FORMAT: 'dd/MM/yyyy',
    DATETIME_FORMAT: 'dd/MM/yyyy HH:mm',
    DATE_ISO_FORMAT: 'yyyy-MM-dd',

    // Turnos
    TURNOS: {
        M: 'Manhã',
        T: 'Tarde',
        N: 'Noite',
    },

    // Situações de Atividade
    SITUACOES_ATIVIDADE: {
        P: 'Pendente',
        C: 'Validada',
        F: 'Fechada (Folha)',
    },

    // Situações de Conselheiro
    SITUACOES_CONSELHEIRO: {
        A: 'Ativo',
        I: 'Inativo',
    },

    // Status de Jeton
    STATUS_JETON: {
        A: 'Calculado (Aberto)',
        E: 'Homologado (Folha)',
    },
};

// MENSAGENS DO SISTEMA

export const MESSAGES = {
    // Confirmações
    CONFIRM_EXCLUSAO: (nome) => `Deseja excluir permanentemente ${nome}?`,
    CONFIRM_VALIDAR: 'Deseja realmente validar esta atividade?',
    CONFIRM_DESVALIDAR: 'Deseja realmente desvalidar esta atividade?',
    CONFIRM_REVOGAR: 'Deseja realmente revogar este registro?',
    CONFIRM_RESTAURAR: 'Deseja realmente restaurar este registro?',

    // Erros
    ERROR_GENERIC: 'Ocorreu um erro inesperado. Tente novamente.',
    ERROR_LOADING_DATA: 'Erro ao carregar os dados.',
    ERROR_SAVING: 'Erro ao salvar os dados.',
    ERROR_DELETING: 'Erro ao excluir o registro.',

    // Sucesso
    SUCCESS_SAVED: 'Registro salvo com sucesso!',
    SUCCESS_DELETED: 'Registro excluído com sucesso!',
    SUCCESS_VALIDATED: 'Atividade validada com sucesso!',
    SUCCESS_HOMOLOGATED: 'Folha homologada com sucesso!',
};

// EXPORTA TUDO EM UM ÚNICO OBJETO (OPCIONAL)

export default {
    API,
    ROUTES,
    CONSTANTS,
    MESSAGES,
};