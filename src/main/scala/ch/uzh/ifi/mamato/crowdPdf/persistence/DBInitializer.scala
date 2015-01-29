package ch.uzh.ifi.mamato.crowdPdf.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.mamato.crowdPdf.util.LazyLogger
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
            sql"CREATE TABLE papers (id BIGINT NOT NULL AUTO_INCREMENT,paper_id BIGINT NOT NULL,PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO papers(paper_id) values (1000);".execute.apply()
          }
          logger.debug("Table Papers created!")
      }

      try {
        sql"select 1 from questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Questions already initialized")
      } catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE questions (id BIGINT NOT NULL AUTO_INCREMENT,question VARCHAR(255) NOT NULL,questiontype VARCHAR(255) NOT NULL,reward INT NOT NULL,created BIGINT NOT NULL,paper_fk BIGINT NOT NULL,question_id BIGINT NOT NULL,PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO questions(question, questiontype, reward, created, paper_fk, question_id) values ('Test question','Boolean',1,123123123,1,1);".execute.apply()
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