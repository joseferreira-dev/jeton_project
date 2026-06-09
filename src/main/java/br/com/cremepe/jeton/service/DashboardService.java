package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private ComprovanteRepository comprovanteRepository;

    public long getTotalAtividadesPendentes() {
        return atividadeRepository.countByInSituacao("P");
    }

    public long getTotalConselheirosAtivos() {
        return conselheiroRepository.countByInSituacao("A");
    }

    public long getTotalAtividadesDoMes() {
        LocalDate hoje = LocalDate.now();
        return atividadeRepository.countAtividadesDoMes(hoje.getMonthValue(), hoje.getYear());
    }

    public long getTotalComprovantes() {
        return comprovanteRepository.count();
    }

    public List<AtividadeConselhal> getUltimasAtividades(int limit) {
        return atividadeRepository.findTop5ByOrderByDataHoraRegistroDesc();
    }
}