package it.scannerposte.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Un aggregato = un lotto di fino a 600 Data Matrix. L'id (autoincrement) e' il numero progressivo. */
@Entity(tableName = "aggregati")
data class Aggregato(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creatoIl: Long
)

@Entity(
    tableName = "codici",
    foreignKeys = [ForeignKey(
        entity = Aggregato::class,
        parentColumns = ["id"],
        childColumns = ["aggregatoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("aggregatoId"), Index(value = ["aggregatoId", "valore"], unique = true)]
)
data class Codice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aggregatoId: Long,
    val valore: String,
    val scansionatoIl: Long,
    /** Percorso del file scan (PNG del Data Matrix) nella cartella privata dell'app. */
    val file: String
)

data class AggregatoConConteggio(val id: Long, val creatoIl: Long, val conteggio: Int)

@Dao
interface ScannerDao {
    @Insert
    suspend fun inserisciAggregato(a: Aggregato): Long

    @Query(
        "SELECT a.id AS id, a.creatoIl AS creatoIl, " +
            "(SELECT COUNT(*) FROM codici c WHERE c.aggregatoId = a.id) AS conteggio " +
            "FROM aggregati a ORDER BY a.id"
    )
    fun aggregati(): Flow<List<AggregatoConConteggio>>

    @Query("SELECT * FROM codici WHERE aggregatoId = :id ORDER BY id")
    fun codici(id: Long): Flow<List<Codice>>

    @Query("SELECT * FROM codici WHERE aggregatoId = :id ORDER BY id")
    suspend fun codiciOra(id: Long): List<Codice>

    @Query("SELECT * FROM codici WHERE aggregatoId = :id AND valore = :valore LIMIT 1")
    suspend fun trova(id: Long, valore: String): Codice?

    /** Cerca il codice in TUTTI gli aggregati, dando la precedenza a quello corrente. */
    @Query("SELECT * FROM codici WHERE valore = :valore ORDER BY (aggregatoId = :id) DESC, id LIMIT 1")
    suspend fun trovaOvunque(id: Long, valore: String): Codice?

    @Query("SELECT COUNT(*) FROM codici WHERE aggregatoId = :id")
    suspend fun conta(id: Long): Int

    @Insert
    suspend fun inserisciCodice(c: Codice): Long

    @Query("DELETE FROM codici WHERE id = :id")
    suspend fun eliminaCodice(id: Long)

    @Query("DELETE FROM aggregati WHERE id = :id")
    suspend fun eliminaAggregato(id: Long)
}

@Database(entities = [Aggregato::class, Codice::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun dao(): ScannerDao
}
