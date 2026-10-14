document.querySelectorAll('[data-confirm-delete]').forEach(form => {
    form.addEventListener('submit', event => {
        if (!window.confirm('Supprimer ce programmeur ? Cette action est définitive.')) {
            event.preventDefault();
        }
    });
});
