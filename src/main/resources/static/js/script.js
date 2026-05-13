function efeitoTelaValidacao(xhr, status, args, dialogo, tabela) {
    if(args.validationFailed) {
        PF(dialogo).jq.effect("shake", {times:5}, 100);
    }
    else {
        PF(dialogo).hide();
        //PF(tabela).clearFilters();
    }
}
