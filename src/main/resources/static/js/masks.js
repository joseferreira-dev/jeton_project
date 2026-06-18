/**
 * masks.js
 * Aplica máscaras em campos usando IMask
 */

// Mapeamento de máscaras (sem money)
const MASK_CONFIGS = {
    cpf: {
        mask: '000.000.000-00',
        lazy: false,
    },
    crm: {
        mask: '00000',
        lazy: false,
    },
    phone: {
        mask: '(00) 00000-0000',
        lazy: false,
    },
    date: {
        mask: '00/00/0000',
        lazy: false,
        blocks: {
            d: { mask: IMask.MaskedRange, from: 1, to: 31 },
            m: { mask: IMask.MaskedRange, from: 1, to: 12 },
            Y: { mask: IMask.MaskedRange, from: 1900, to: 2100 },
        },
    },
    cep: {
        mask: '00000-000',
        lazy: false,
    },
    number: {
        mask: Number,
        thousandsSeparator: '.',
        radix: ',',
        scale: 2,
        normalizeZeros: true,
        mapToRadix: ['.'],
    },
};

/**
 * Aplica máscara a um campo individual
 */
export function applyMask(field, maskType) {
    if (!field || !maskType) return;
    if (!MASK_CONFIGS[maskType]) {
        console.warn(`Máscara desconhecida: ${maskType}`);
        return;
    }

    // Remove máscara anterior se existir
    if (field._imask) {
        field._imask.destroy();
        delete field._imask;
    }

    try {
        const config = MASK_CONFIGS[maskType];
        const imask = IMask(field, config);
        field._imask = imask;
        return imask;
    } catch (error) {
        console.error(`Erro ao aplicar máscara ${maskType}:`, error);
    }
}

/**
 * Inicializa todas as máscaras nos campos com data-mask
 */
export function initMasks() {
    const fields = document.querySelectorAll('[data-mask]');
    fields.forEach(field => {
        const maskType = field.getAttribute('data-mask');
        const mask = applyMask(field, maskType);
        if (mask && field.value) {
            mask.updateValue();
        }
    });
}

/**
 * Atualiza a máscara de um campo após alteração
 */
export function refreshMask(field) {
    const maskType = field.getAttribute('data-mask');
    if (maskType) {
        applyMask(field, maskType);
        if (field.value) {
            field.dispatchEvent(new Event('input', { bubbles: true }));
        }
    }
}

/**
 * Obtém o valor limpo (sem formatação) de um campo com máscara
 */
export function getRawValue(field) {
    if (field._imask) {
        return field._imask.unmaskedValue;
    }
    return field.value.replace(/\D/g, '');
}

export { MASK_CONFIGS };