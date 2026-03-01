CREATE TABLE IF NOT EXISTS reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    sujet VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reponse (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reclamation_id INT NOT NULL,
    admin_id INT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reponse_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation(id) ON DELETE CASCADE,
    CONSTRAINT fk_reponse_admin FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE CASCADE
);
