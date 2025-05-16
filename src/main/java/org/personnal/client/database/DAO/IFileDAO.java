package org.personnal.client.database.DAO;

import org.personnal.client.model.FileData;
import java.util.List;

public interface IFileDAO {
    void saveFile(FileData file);
    List<FileData> getFilesWith(String username);
    void deleteFileById(int id);
}
