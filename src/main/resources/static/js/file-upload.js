/**
 * file-upload.js
 * Melhoria no upload de arquivos: exibe nome, tamanho e pré-visualização
 */

export function initFileUploads() {
    document.querySelectorAll('[data-role="file-upload"]').forEach(input => {
        // Remove listeners antigos
        input.removeEventListener('change', handleFileChange);
        input.addEventListener('change', handleFileChange);

        // Procura o container pai para a pré-visualização
        const container = input.closest('.file-upload-container');
        if (!container) return;

        // Se já existe um preview, não recria
        if (container.querySelector('.file-preview')) return;

        // Cria os elementos de preview
        const previewDiv = document.createElement('div');
        previewDiv.className = 'file-preview mt-2';

        // Informações do arquivo
        const infoDiv = document.createElement('div');
        infoDiv.className = 'file-info small text-muted mb-1';
        infoDiv.innerHTML = '<span class="file-name">Nenhum arquivo selecionado</span> <span class="file-size"></span>';
        previewDiv.appendChild(infoDiv);

        // Container para visualização (imagem/PDF)
        const visualContainer = document.createElement('div');
        visualContainer.className = 'file-visual-container';
        visualContainer.style.maxHeight = '300px';
        visualContainer.style.overflow = 'auto';
        visualContainer.style.display = 'none';
        previewDiv.appendChild(visualContainer);

        // Botão remover
        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'btn btn-sm btn-outline-danger mt-1';
        removeBtn.innerHTML = '<i class="fa-solid fa-xmark me-1"></i> Remover arquivo';
        removeBtn.style.display = 'none';
        removeBtn.addEventListener('click', function (e) {
            e.preventDefault();
            input.value = '';
            clearPreview(container);
            // Dispara evento change para validação
            input.dispatchEvent(new Event('change', { bubbles: true }));
        });
        previewDiv.appendChild(removeBtn);

        container.appendChild(previewDiv);

        // Se já houver arquivo selecionado (ex: edição), exibe
        if (input.files && input.files.length > 0) {
            handleFileChange.call(input, { target: input });
        }
    });
}

function handleFileChange(e) {
    const input = e.target;
    const container = input.closest('.file-upload-container');
    if (!container) return;

    const file = input.files && input.files[0];
    const previewDiv = container.querySelector('.file-preview');
    if (!previewDiv) return;

    const infoDiv = previewDiv.querySelector('.file-info');
    const visualDiv = previewDiv.querySelector('.file-visual-container');
    const removeBtn = previewDiv.querySelector('.btn-outline-danger');

    if (!file) {
        clearPreview(container);
        return;
    }

    // Exibe informações
    const nameSpan = infoDiv.querySelector('.file-name');
    const sizeSpan = infoDiv.querySelector('.file-size');
    if (nameSpan) nameSpan.textContent = `📎 ${file.name}`;
    if (sizeSpan) {
        const size = (file.size / 1024).toFixed(1);
        sizeSpan.textContent = `(${size} KB)`;
        sizeSpan.style.color = '#6c757d';
    }

    // Exibe pré-visualização
    visualDiv.style.display = 'block';
    visualDiv.innerHTML = '';

    const fileType = file.type;
    const isImage = fileType.startsWith('image/');
    const isPDF = fileType === 'application/pdf';

    if (isImage) {
        const reader = new FileReader();
        reader.onload = function (ev) {
            const img = document.createElement('img');
            img.src = ev.target.result;
            img.className = 'img-fluid rounded border';
            img.style.maxHeight = '250px';
            img.style.maxWidth = '100%';
            img.style.objectFit = 'contain';
            visualDiv.appendChild(img);
        };
        reader.readAsDataURL(file);
    } else if (isPDF) {
        // Para PDF, mostra ícone e link para visualizar (não podemos embutir facilmente)
        const pdfIcon = document.createElement('div');
        pdfIcon.className = 'text-center p-3 border rounded bg-light';
        pdfIcon.innerHTML = `
            <i class="fa-solid fa-file-pdf text-danger" style="font-size: 3rem;"></i>
            <p class="mt-1 mb-0 small">Arquivo PDF</p>
            <p class="small text-muted">Clique em "Remover arquivo" para trocar.</p>
        `;
        visualDiv.appendChild(pdfIcon);

        // Opcional: tentar carregar o PDF em iframe (mas pode não funcionar com blob local)
        // Para simplificar, exibimos apenas o ícone.
    } else {
        // Outros tipos: ícone genérico
        const genericIcon = document.createElement('div');
        genericIcon.className = 'text-center p-3 border rounded bg-light';
        genericIcon.innerHTML = `
            <i class="fa-solid fa-file" style="font-size: 3rem;"></i>
            <p class="mt-1 mb-0 small">Arquivo ${fileType || 'desconhecido'}</p>
        `;
        visualDiv.appendChild(genericIcon);
    }

    // Mostra botão remover
    if (removeBtn) removeBtn.style.display = 'inline-block';

    // Atualiza validação (se houver)
    if (input.hasAttribute('data-validate')) {
        // Dispara um evento para revalidar
        input.dispatchEvent(new Event('blur', { bubbles: true }));
    }
}

function clearPreview(container) {
    const previewDiv = container.querySelector('.file-preview');
    if (!previewDiv) return;

    const infoDiv = previewDiv.querySelector('.file-info');
    const nameSpan = infoDiv?.querySelector('.file-name');
    const sizeSpan = infoDiv?.querySelector('.file-size');
    if (nameSpan) nameSpan.textContent = 'Nenhum arquivo selecionado';
    if (sizeSpan) sizeSpan.textContent = '';

    const visualDiv = previewDiv.querySelector('.file-visual-container');
    if (visualDiv) {
        visualDiv.style.display = 'none';
        visualDiv.innerHTML = '';
    }

    const removeBtn = previewDiv.querySelector('.btn-outline-danger');
    if (removeBtn) removeBtn.style.display = 'none';
}

// Se houver um arquivo previamente carregado (via atributo data-current-file), exibe
// Isso pode ser usado na edição, mas como o backend não envia o arquivo, deixamos para o usuário ver o nome do arquivo atual.
// No template, podemos exibir o nome do arquivo atual com um badge.