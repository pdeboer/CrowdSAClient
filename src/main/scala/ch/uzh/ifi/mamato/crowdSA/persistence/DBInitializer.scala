package ch.uzh.ifi.mamato.crowdSA.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import scalikejdbc._

object DBInitializer extends LazyLogger{

  def run() {
    DB readOnly { implicit s =>
      try {
        sql"select 1 from papers limit 1".map(_.long(1)).single.apply()
        logger.debug("Papers already initialized")
      } catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE papers (id BIGINT NOT NULL AUTO_INCREMENT,title VARCHAR(255) NOT NULL, budget_cts INT NOT NULL, remote_id BIGINT NOT NULL, PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO papers(title, budget_cts, remote_id) values ('Test paper', 1000, 1);".execute.apply()
          }
          logger.debug("Table Papers created!")
      }

      try {
        sql"select 1 from questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Questions already initialized")
      } catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE questions (id BIGINT NOT NULL AUTO_INCREMENT,question VARCHAR(255) NOT NULL,question_type VARCHAR(255) NOT NULL,reward_cts INT NOT NULL,created_at BIGINT NOT NULL,remote_paper_id BIGINT NOT NULL,remote_question_id BIGINT NOT NULL, disabled BIT NOT NULL, maximal_assignments INT NULL, expiration_time_sec BIGINT NULL,PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO questions(question, question_type, reward_cts, created_at, remote_paper_id, remote_question_id, disabled, maximal_assignments, expiration_time_sec) values ('Test question','Boolean',10,123123123,1,1, false, NULL, NULL);".execute.apply()
          }
          logger.debug("Table Questions created!")
      }

    }
  }

}

/*create sequence company_id_seq start with 1;
create table company (
id bigint not null default nextval('company_id_seq') primary key,
name varchar(255) not null,
url varchar(255),
created_at timestamp not null,
deleted_at timestamp
);
create sequence skill_id_seq start with 1;
create table skill (
id bigint not null default nextval('skill_id_seq') primary key,
name varchar(255) not null,
created_at timestamp not null,
deleted_at timestamp
);
create table programmer_skill (
programmer_id bigint not null,
skill_id bigint not null,
primary key(programmer_id, skill_id)
);

insert into skill (name, created_at) values ('Scala', current_timestamp);
insert into skill (name, created_at) values ('Java', current_timestamp);
insert into skill (name, created_at) values ('Ruby', current_timestamp);
insert into skill (name, created_at) values ('MySQL', current_timestamp);
insert into skill (name, created_at) values ('PostgreSQL', current_timestamp);
*/