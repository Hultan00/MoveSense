import android.os.Environment
import com.example.accmon.data.Fusion
import com.example.accmon.data.Gyro
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataExport {

    companion object {

        fun getCurrentDateTime(): String {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
            return currentDateTime.format(formatter)
        }

        fun formatFusionData(fusionData: ArrayList<Fusion>): ArrayList<String>{
            var formatedList = ArrayList<String>()
            for (data in fusionData){
                var string = data.ms.toString() + ","
                string += data.p.toString()
                formatedList.add(string)
            }
            return formatedList
        }

        fun formatAccData(fusionData: ArrayList<Acc>): ArrayList<String>{
            var formatedList = ArrayList<String>()
            for (data in fusionData){
                var string = data.ms.toString() + ","
                string += data.p.toString()
                formatedList.add(string)
            }
            return formatedList
        }

        fun formatGyroData(fusionData: ArrayList<Gyro>): ArrayList<String>{
            var formatedList = ArrayList<String>()
            for (data in fusionData){
                var string = data.ms.toString() + ","
                string += data.xa.toString()
                formatedList.add(string)
            }
            return formatedList
        }

        fun exportToCsv(data: ArrayList<String>, filePath: String) {
            try {
                val file = File(filePath)

                // Create the file if it doesn't exist
                if (!file.exists()) {
                    file.createNewFile()
                }

                // Open a FileWriter to write to the file
                val fileWriter = FileWriter(file)

                // Write data to the file
                for (line in data) {
                    fileWriter.append(line)
                    fileWriter.append("\n")
                }

                // Close the FileWriter
                fileWriter.flush()
                fileWriter.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getDefaultExportFilePath(fileName: String): String {
            val documentsDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(documentsDirectory, fileName)
            return file.absolutePath
        }
    }
}
