/**
 * validation.js
 * Validação de formulários em tempo real (frontend)
 * Feedback visual com classes .is-valid / .is-invalid e mensagens de erro
 */

// ============================================================================
// CONFIGURAÇÃO DE MENSAGENS
// ============================================================================

const MESSAGES = {
    required: 'Este campo é obrigatório.',
    cpf: 'CPF inválido. Deve ter 11 dígitos e ser válido.',
    crm: 'CRM inválido. Apenas números.',
    email: 'E-mail inválido.',
    number: 'Digite um número válido.',
    integer: 'Digite um número inteiro.',
    min: (val) => `Valor mínimo é ${val}.`,
    max: (val) => `Valor máximo é ${val}.`,
    date: 'Data inválida.',
    datetime: 'Data e hora inválidas.',
    minlength: (val) => `Mínimo de ${val} caracteres.`,
    maxlength: (val) => `Máximo de ${val} caracteres.`,
    pattern: 'Formato inválido.',
};

// ============================================================================
// FUNÇÕES DE VALIDAÇÃO (cada uma retorna string | null)
// ============================================================================

const validators = {

    required(value) {
        if (value === undefined || value === null || value.trim() === '') {
            return MESSAGES.required;
        }
        return null;
    },

    cpf(value) {
        if (!value) return null; // required cuida disso
        const cleaned = value.replace(/\D/g, '');
        if (cleaned.length !== 11) return MESSAGES.cpf;
        // Validação básica (evita todos dígitos iguais)
        if (/^(\d)\1{10}$/.test(cleaned)) return MESSAGES.cpf;
        // Valida dígitos verificadores
        let sum = 0;
        for (let i = 0; i < 9; i++) {
            sum += parseInt(cleaned.charAt(i)) * (10 - i);
        }
        let rev = 11 - (sum % 11);
        if (rev === 10 || rev === 11) rev = 0;
        if (rev !== parseInt(cleaned.charAt(9))) return MESSAGES.cpf;
        sum = 0;
        for (let i = 0; i < 10; i++) {
            sum += parseInt(cleaned.charAt(i)) * (11 - i);
        }
        rev = 11 - (sum % 11);
        if (rev === 10 || rev === 11) rev = 0;
        if (rev !== parseInt(cleaned.charAt(10))) return MESSAGES.cpf;
        return null;
    },

    crm(value) {
        if (!value) return null;
        if (!/^\d+$/.test(value)) return MESSAGES.crm;
        return null;
    },

    email(value) {
        if (!value) return null;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) return MESSAGES.email;
        return null;
    },

    number(value, field) {
        if (!value) return null;
        const cleanValue = field ? getCleanValue(field) : value;
        const normalized = String(cleanValue).replace(/[^\d.]/g, '');
        if (isNaN(normalized) || normalized.trim() === '') return MESSAGES.number;
        return null;
    },

    min(value, param, field) {
        if (!value) return null;
        const cleanValue = field ? getCleanValue(field) : value;
        const normalized = String(cleanValue).replace(/[^\d.]/g, '');
        const num = parseFloat(normalized);
        if (isNaN(num) || num < param) return MESSAGES.min(param);
        return null;
    },

    max(value, param, field) {
        if (!value) return null;
        const cleanValue = field ? getCleanValue(field) : value;
        const normalized = String(cleanValue).replace(/[^\d.]/g, '');
        const num = parseFloat(normalized);
        if (isNaN(num) || num > param) return MESSAGES.max(param);
        return null;
    },

    integer(value) {
        if (!value) return null;
        if (!/^-?\d+$/.test(value)) return MESSAGES.integer;
        return null;
    },

    minlength(value, param) {
        if (!value) return null;
        if (value.length < param) return MESSAGES.minlength(param);
        return null;
    },

    maxlength(value, param) {
        if (!value) return null;
        if (value.length > param) return MESSAGES.maxlength(param);
        return null;
    },

    pattern(value, param) {
        if (!value) return null;
        const regex = new RegExp(param);
        if (!regex.test(value)) return MESSAGES.pattern;
        return null;
    },

    date(value) {
        if (!value) return null;
        const d = new Date(value);
        if (isNaN(d.getTime())) return MESSAGES.date;
        return null;
    },

    datetime(value) {
        if (!value) return null;
        const d = new Date(value);
        if (isNaN(d.getTime())) return MESSAGES.datetime;
        return null;
    },
};

// ============================================================================
// FUNÇÃO PRINCIPAL DE VALIDAÇÃO DE CAMPO
// ============================================================================

/**
 * Valida um campo com base em seus atributos data-validate
 * @param {HTMLElement} field - O campo de input/select/textarea
 * @param {boolean} showFeedback - Se deve exibir mensagem de erro abaixo do campo
 * @returns {boolean} true se válido, false caso contrário
 */
export function validateField(field, showFeedback = true) {
    const rules = field.getAttribute('data-validate');
    if (!rules) return true;

    const value = field.type === 'checkbox' || field.type === 'radio'
        ? field.checked
        : field.value;

    // Remove qualquer feedback existente
    clearFeedback(field);

    const ruleList = rules.split(/\s+/);
    let firstError = null;

    for (const rule of ruleList) {
        // Suporte a parâmetros: min:3, max:10, pattern:...
        let [ruleName, param] = rule.split(':');
        param = param !== undefined ? param : null;

        if (ruleName === 'required') {
            // Para checkbox/radio, verifica se está marcado
            if (field.type === 'checkbox' || field.type === 'radio') {
                if (!field.checked) {
                    firstError = MESSAGES.required;
                    break;
                }
            } else {
                const error = validators.required(value);
                if (error) { firstError = error; break; }
            }
            continue;
        }

        // Se o campo está vazio e não é required, pula as outras validações (exceto se houver value)
        if (!value && ruleName !== 'required') continue;

        const validator = validators[ruleName];
        if (!validator) {
            console.warn(`Validador desconhecido: ${ruleName}`);
            continue;
        }

        let error;
        if (param !== null) {
            error = validator(value, param, field);
        } else {
            error = validator(value, field);
        }

        if (error) {
            firstError = error;
            break;
        }
    }

    if (firstError) {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
        if (showFeedback) {
            showFeedbackMessage(field, firstError);
        }
        return false;
    } else {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
        return true;
    }
}

// ============================================================================
// FUNÇÕES AUXILIARES DE FEEDBACK
// ============================================================================

function showFeedbackMessage(field, message) {
    // Tenta encontrar um elemento de feedback existente (div.feedback-msg)
    let feedback = field.parentElement.querySelector('.feedback-msg');
    if (!feedback) {
        feedback = document.createElement('div');
        feedback.className = 'feedback-msg invalid-feedback';
        feedback.style.display = 'block';
        field.parentElement.appendChild(feedback);
    }
    feedback.textContent = message;
    feedback.style.display = 'block';
}

function clearFeedback(field) {
    field.classList.remove('is-valid', 'is-invalid');
    const feedback = field.parentElement.querySelector('.feedback-msg');
    if (feedback) {
        feedback.style.display = 'none';
    }
}

function getCleanValue(field) {
    if (field._imask) {
        return field._imask.unmaskedValue;
    }
    return field.value;
}

// ============================================================================
// VALIDAÇÃO DE FORMULÁRIO COMPLETO (antes do submit)
// ============================================================================

/**
 * Valida todos os campos com data-validate dentro de um formulário
 * @param {HTMLFormElement} form
 * @returns {boolean} true se todos os campos forem válidos
 */
export function validateForm(form) {
    const fields = form.querySelectorAll('[data-validate]');
    let valid = true;
    for (const field of fields) {
        const isValid = validateField(field, true);
        if (!isValid) valid = false;
    }
    return valid;
}

// ============================================================================
// INICIALIZAÇÃO: ADICIONA EVENTOS AOS CAMPOS
// ============================================================================

export function initValidation() {
    document.querySelectorAll('[data-validate]').forEach(field => {
        // Remove listeners antigos (evita duplicidade)
        field.removeEventListener('blur', handleBlur);
        field.removeEventListener('input', handleInput);
        field.removeEventListener('change', handleChange);

        field.addEventListener('blur', handleBlur);
        field.addEventListener('input', handleInput);
        field.addEventListener('change', handleChange);
    });
}

function handleBlur(e) {
    const field = e.target;
    // Valida ao sair do campo (mostra feedback)
    validateField(field, true);
}

function handleInput(e) {
    const field = e.target;
    // Validação em tempo real (sem mostrar mensagem de erro, apenas classe)
    // Remove a mensagem de erro, mas mantém a classe de inválido se ainda estiver errado?
    // Para não sobrecarregar, limpamos o feedback e revalidamos sem mensagem.
    // Mas podemos mostrar a mensagem apenas no blur.
    // Vamos revalidar para atualizar a classe, mas sem mensagem.
    clearFeedback(field);
    validateField(field, false);
}

function handleChange(e) {
    const field = e.target;
    // Para selects e checkboxes, valida e mostra feedback (opcional)
    validateField(field, true);
}

// ============================================================================
// EXPOSIÇÃO GLOBAL PARA USO EM CONSOLE (opcional)
// ============================================================================

window.validateField = validateField;
window.validateForm = validateForm;
