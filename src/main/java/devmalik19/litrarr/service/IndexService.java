package devmalik19.litrarr.service;

import devmalik19.litrarr.data.dao.Index;
import devmalik19.litrarr.repository.IndexRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndexService
{
    @Autowired
	private IndexRepository indexRepository;

    public List<Index> findAll()
    {
        return indexRepository.findAll();
    }

}
