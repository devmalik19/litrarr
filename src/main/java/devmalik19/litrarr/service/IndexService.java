package devmalik19.litrarr.service;

import devmalik19.litrarr.data.dao.Index;
import devmalik19.litrarr.repository.IndexRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IndexService
{
	private final IndexRepository indexRepository;

	public IndexService(IndexRepository indexRepository)
	{
		this.indexRepository = indexRepository;
	}

	public List<Index> findAll()
	{
		return indexRepository.findAll();
	}
}
