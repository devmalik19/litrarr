package devmalik19.litrarr.service.metadata;

import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import org.springframework.stereotype.Service;

@Service
public class SampleService
{

	public String get()
	{
		return "{"
			+ "  \"created\": \"2025-12-19T11:13:00.000Z\","
			+ "  \"count\": 14,"
			+ "  \"offset\": 0,"
			+ "  \"release-groups\": ["
			+ "    {"
			+ "      \"id\": \"f32fab67-77dd-3937-addc-9062e28e4c37\","
			+ "      \"type-id\": \"f529b476-6e62-324f-b0aa-1f3eeaa27027\","
			+ "      \"score\": 100,"
			+ "      \"primary-type\": \"Album\","
			+ "      \"title\": \"Thriller\","
			+ "      \"first-release-date\": \"1982-11-30\","
			+ "      \"artist-credit\": ["
			+ "        {"
			+ "          \"name\": \"Michael Jackson\","
			+ "          \"artist\": {"
			+ "            \"id\": \"f27ec0db-af05-4f36-916e-3d5ef745fe9e\","
			+ "            \"name\": \"Michael Jackson\","
			+ "            \"sort-name\": \"Jackson, Michael\""
			+ "          }"
			+ "        }"
			+ "      ],"
			+ "      \"tags\": ["
			+ "        { \"count\": 5, \"name\": \"pop\" },"
			+ "        { \"count\": 3, \"name\": \"funk\" }"
			+ "      ]"
			+ "    }"
			+ "  ]"
			+ "}";
	}

	public void getMetaForLibrary(Library library)
	{
	}

	public void getMetaForItem(Item item)
	{

	}
}
