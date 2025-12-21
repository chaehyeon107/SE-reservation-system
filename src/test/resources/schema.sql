DROP TABLE IF EXISTS seat_reservation CASCADE;
DROP TABLE IF EXISTS room_reservation_participant CASCADE;
DROP TABLE IF EXISTS room_reservation CASCADE;
DROP TABLE IF EXISTS student CASCADE;
DROP TABLE IF EXISTS seat CASCADE;
DROP TABLE IF EXISTS room CASCADE;

CREATE TABLE student (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         student_id BIGINT NOT NULL UNIQUE,
                         name VARCHAR(100),
                         seat_daily_used_hours INT DEFAULT 0,
                         usage_date DATE,
                         usage_week_start DATE,
                         meeting_daily_used_hours INT DEFAULT 0,
                         meeting_weekly_used_hours INT DEFAULT 0,
                         seat_usage_date DATE
);

CREATE TABLE seat (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      seat_number INT NOT NULL,
                      status VARCHAR(50) NOT NULL
);

CREATE TABLE room (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      capacity INT NOT NULL
);

CREATE TABLE seat_reservation (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  seat_id BIGINT,
                                  student_id BIGINT,
                                  date DATE NOT NULL,
                                  start_time TIME NOT NULL,
                                  end_time TIME NOT NULL,
                                  created_at TIMESTAMP,
                                  updated_at TIMESTAMP
);

CREATE TABLE room_reservation (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  room_id BIGINT,
                                  representative_id BIGINT,
                                  leader_student_id BIGINT,
                                  date DATE NOT NULL,
                                  start_time TIME NOT NULL,
                                  end_time TIME NOT NULL,
                                  duration INT NOT NULL,
                                  status VARCHAR(50) NOT NULL
);

CREATE TABLE room_reservation_participant (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              reservation_id BIGINT NOT NULL,
                                              student_id BIGINT NOT NULL,
                                              is_representative BOOLEAN DEFAULT FALSE
);