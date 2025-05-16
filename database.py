import sqlite3

class DatabaseManager:
    def __init__(self, db_path: str):
        self.conn = sqlite3.connect(db_path)
        self.create_tables()

    def create_tables(self):
        cursor = self.conn.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                email TEXT
            );
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                email TEXT
            );
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                receiver TEXT NOT NULL,
                content TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_sent_by_me BOOLEAN NOT NULL
            );
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender TEXT NOT NULL,
                receiver TEXT NOT NULL,
                filename TEXT NOT NULL,
                filepath TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                is_sent_by_me BOOLEAN NOT NULL
            );
        """)

        self.conn.commit()
        cursor.close()

    def close(self):
        self.conn.close()
if __name__ == "__main__":
    db = DatabaseManager("client_chat.db")
    print("✅ Base de données initialisée avec succès.")
    db.close()
