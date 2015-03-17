package ch.uzh.ifi.mamato.crowdSA.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import scalikejdbc._

object DBInitializer extends LazyLogger{

  def run() {
    DB readOnly { implicit s =>
      //Papers TABLE
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

      //discovery TABLE
      try {
        sql"select 1 from shortn limit 1".map(_.long(1)).single.apply()
        logger.debug("Discovery already initialized")
      } catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE shortn (id BIGINT NOT NULL AUTO_INCREMENT,description VARCHAR(10000) NULL, start_time varchar(255) NULL, end_time varchar(255) NULL, result VARCHAR(255) NULL, error VARCHAR(10000) NULL, cost INT NULL, PRIMARY KEY(id));".execute().apply()
            //sql"INSERT INTO shortn(description, budget_cts, remote_id) values ('Test paper', 1000, 1);".execute.apply()
          }
          logger.debug("Table Discovery created!")
      }

      //Questions TABLE
      try {
        sql"select 1 from questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Questions already initialized")
      } catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE questions (id BIGINT NOT NULL AUTO_INCREMENT,question VARCHAR(255) NOT NULL,question_type VARCHAR(255) NOT NULL,reward_cts INT NOT NULL,created_at BIGINT NOT NULL,remote_paper_id BIGINT NOT NULL,remote_question_id BIGINT NOT NULL, disabled BIT NOT NULL, maximal_assignments INT NULL, expiration_time_sec BIGINT NULL, possible_answers VARCHAR(2000) NULL, PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO questions(question, question_type, reward_cts, created_at, remote_paper_id, remote_question_id, disabled, maximal_assignments, expiration_time_sec, possible_answers) values ('Test question','Boolean',10,123123123,1,1, false, NULL, NULL, NULL);".execute.apply()
          }
          logger.debug("Table Questions created!")
      }

      //Stat_methods TABLE
      try {
        sql"select 1 from stat_methods limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      } catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE stat_methods (id BIGINT NOT NULL AUTO_INCREMENT, stat_method VARCHAR(255) NOT NULL, PRIMARY KEY(id));".execute().apply()
            sql"INSERT INTO stat_methods(stat_method) values ('ANOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('MANOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('ANCOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('linear regression');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('logistic regression');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('correlation');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('pearson correlation');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mann-whitney test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mann-whitney u test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('wilcoxon signed-ranks test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('wilcoxon paired signed rank test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('krusakal-wallis test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mcnamar');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('friedman');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for population variance');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for an assumed population variance');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square test for compatibility of K counts');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a 2x2 table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a kx2 table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a 2xk table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a pxq table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for a suitable probabilistic model');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test for two population means');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test for a population mean');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test of a correlation coefficient');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test of a regression coefficient');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('factor analysis');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('cluster analysis');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('survival analysis');".execute.apply()
          }
          logger.debug("Table Stat_methods created!")
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