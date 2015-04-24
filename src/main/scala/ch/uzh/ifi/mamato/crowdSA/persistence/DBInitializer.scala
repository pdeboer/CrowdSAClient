package ch.uzh.ifi.mamato.crowdSA.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import scalikejdbc._

object DBInitializer extends LazyLogger {

  def run() {
    DB readOnly { implicit s =>
      //Papers TABLE
      try {
        sql"select 1 from papers limit 1".map(_.long(1)).single.apply()
        logger.debug("Papers already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE papers (id BIGINT NOT NULL AUTO_INCREMENT,title VARCHAR(255) NOT NULL, budget_cts INT NOT NULL, remote_id BIGINT NOT NULL, PRIMARY KEY(id));".execute().apply()
          }
          logger.debug("Table Papers created!")
      }

      //discovery TABLE
      try {
        sql"select 1 from discovery limit 1".map(_.long(1)).single.apply()
        logger.debug("Discovery already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE discovery (id BIGINT NOT NULL AUTO_INCREMENT,description VARCHAR(10000) NULL, paper_id BIGINT NULL, start_time varchar(100) NULL, end_time varchar(100) NULL, result TEXT NULL, error TEXT NULL, cost INT NULL, PRIMARY KEY(id));".execute().apply()
          }
          logger.debug("Table Discovery created!")
      }

      //Questions TABLE
      try {
        sql"select 1 from questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Questions already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE questions (id BIGINT NOT NULL AUTO_INCREMENT,question VARCHAR(255) NOT NULL,question_type VARCHAR(255) NOT NULL,reward_cts INT NOT NULL,created_at BIGINT NOT NULL,remote_paper_id BIGINT NOT NULL,remote_question_id BIGINT NOT NULL, disabled BIT NOT NULL, maximal_assignments INT NULL, expiration_time_sec BIGINT NULL, possible_answers VARCHAR(2000) NULL, PRIMARY KEY(id));".execute().apply()
          }
          logger.debug("Table Questions created!")
      }

      //Highlight TABLE
      try {
        sql"select 1 from highlights limit 1".map(_.long(1)).single.apply()
        logger.debug("Highlights already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE highlights (id BIGINT NOT NULL AUTO_INCREMENT, assumption VARCHAR(255) NOT NULL, terms VARCHAR(10000) NOT NULL,remote_question_id BIGINT NOT NULL, PRIMARY KEY(id));".execute().apply()
          }
          logger.debug("Table Highlights created!")
      }

      //Answers TABLE
      try {
        sql"select 1 from answers limit 1".map(_.long(1)).single.apply()
        logger.debug("Answers already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE answers (id BIGINT NOT NULL UNIQUE, answer TEXT NOT NULL, created_at BIGINT NOT NULL, accepted BIT NULL, bonus_cts INT NULL, rejected BIT NULL, assignments_id BIGINT NOT NULL, PRIMARY KEY(id));".execute().apply()
          }
          logger.debug("Table Answers created!")
      }

      //Stat_methods TABLE
      try {
        sql"select 1 from stat_methods limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE stat_methods (id BIGINT NOT NULL AUTO_INCREMENT, stat_method VARCHAR(255) NOT NULL, PRIMARY KEY(id));".execute().apply()

            sql"INSERT INTO stat_methods(stat_method) values ('MANOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('ANOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('ANCOVA');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('linear regression');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('logistic regression');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('correlation');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mann-whitney test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('wilcoxon signed-ranks test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('kruskal-wallis test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('mcnemar');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('friedman');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square test');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for population variance');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for an assumed population variance');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square test for compatibility of K counts');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a 2x2 table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a kx2 table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for consistency in a 2xk table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for independence in a pxq table');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('chi square for a suitable probabilistic model');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('t-test for two population means');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test for a population mean');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('t-test of a regression coefficient');".execute.apply()

            sql"INSERT INTO stat_methods(stat_method) values ('t-test of a correlation coefficient');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('factor analysis');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('cluster analysis');".execute.apply()
            sql"INSERT INTO stat_methods(stat_method) values ('survival analysis');".execute.apply()
          }
          logger.debug("Table Stat_methods created!")
      }

      //Assumptions TABLE
      try {
        sql"select 1 from assumptions limit 1".map(_.long(1)).single.apply()
        logger.debug("Assumptions already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE assumptions (id BIGINT NOT NULL AUTO_INCREMENT, assumption VARCHAR(255) NOT NULL, url VARCHAR(255) NULL, PRIMARY KEY(id));".execute().apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Normality', 'http://en.wikipedia.org/wiki/Normality_test');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Linearity', 'http://www.utexas.edu/courses/schwab/sw388r7/SolvingProblems/AssumptionOfLinearity.ppt');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Homogeneity of variances', 'http://en.wikipedia.org/wiki/Homogeneity_%28statistics%29');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Constant Variance', 'https://www.statisticssolutions.com/homoscedasticity/');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Independence', 'http://en.wikipedia.org/wiki/Independence_%28probability_theory%29');".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Homogeneity of regression slopes', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Independence of Error terms', 'http://www.pages.drexel.edu/~tpm23/STAT902/DWTest.pdf');".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Ordinal variables', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Are there interactions between the variables in the dataset?', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('The sample size is greater than 30', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('The sample size is greater than 30', null);".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Variables have to be either interval or ratio measurements', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('The outliers are kept to minimum or removed entierly', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Responses are ordinal', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Random samples are picked from the population', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Data is measured at least on an ordinal scale', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Each pair is chosen randomly and independently', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Continuous distributions are the same for the test variables', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Case represent random samples from the population', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Pair are matched', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Existence of at least one group which is measured on three or more different occasions', null);".execute.apply()


            sql"INSERT INTO assumptions(assumption, url) values ('Random samples', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('One dependant variable is either ordinal, interval or ratio type', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Quantitative data are used', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('The observations are independent', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Adequate sample size (at least N=10)', null);".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Data in frequency form', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('No particular distribution is assumed', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Sample Size larger than 20', null);".execute.apply()

            sql"INSERT INTO assumptions(assumption, url) values ('Cell frequencies greather than 3', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Cell frequencies greather than 5', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('K classes', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Interval classification', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Error has constant variance and it is on average equals 0', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('No association between the factor and measurement error', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('No association between errors', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Sample represent the whole population', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Variables are not correlated', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Non-informative censoring', null);".execute.apply()
            sql"INSERT INTO assumptions(assumption, url) values ('Proportional hazards', null);".execute.apply()
          }
          logger.debug("Table Assumptions created!")
      }

      //Stat_method2Assumptions TABLE
      try {
        sql"select 1 from stat_method2assumptions limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE stat_method2assumptions (id BIGINT NOT NULL AUTO_INCREMENT, stat_method_id BIGINT NOT NULL, assumption_id BIGINT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(assumption_id) REFERENCES assumptions(id), FOREIGN KEY(stat_method_id) REFERENCES stat_methods(id));".execute().apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,2);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,3);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (1,4);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,3);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,4);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (2,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,2);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,3);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,6);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (3,7);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,4);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (4,7);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (5,8);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (5,9);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (5,10);".execute.apply()


            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,4);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,12);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (6,13);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (7,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (7,14);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (7,15);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (8,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (8,16);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (8,17);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (9,5);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (9,19);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (9,18);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (10,20);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,21);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,22);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (11,23);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,24);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,25);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,26);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,22);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (12,27);".execute.apply()

            //chi square for population variance
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (13,1);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (14,1);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (15,28);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (16,29);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (16,30);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (17,31);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (18,29);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (18,32);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (19,31);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (20,33);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (21,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (21,5);".execute.apply()
            // chi square for consistency in a 2xk table
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (22,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (22,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (23,1);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (23,4);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (24,2);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (24,1);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (25,34);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (25,35);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (25,36);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (25,5);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (26,37);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (26,38);".execute.apply()

            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (27,39);".execute.apply()
            sql"INSERT INTO stat_method2assumptions(stat_method_id, assumption_id) values (27,40);".execute.apply()

          }
          logger.debug("Table Stat_methods created!")
      }

      //Assumptions2Questions TABLE
      try {
        sql"select 1 from assumption2questions limit 1".map(_.long(1)).single.apply()
        logger.debug("Stat_methods already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE assumption2questions (id BIGINT NOT NULL AUTO_INCREMENT, assumption_id BIGINT NOT NULL, question VARCHAR(1000) NOT NULL, test_names VARCHAR(255) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(assumption_id) REFERENCES assumptions(id));".execute().apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the De Agostino K-squared test used to test the normality assumption?', 'Agostino,K-squared,K2,K test,kurtosis,skewness');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Jarque-Bera test used to test the normality?', 'Jarque-Bera,JB test');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Anderson-Darling test used to test the normality?', 'Anderson-Darling,K-sample');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Cramér-von Mises criterion used to test the normality?', 'Cramér-von Mises criterion');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Lilliefors test used to test the normality?', 'Lilliefors');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Kolmogorov-Smirnov test used to test the normality?', 'Kolmogorov-Smirnov,K-S test');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Shapiro-Wilk test used to test the normality?', 'Shapiro-Wilk');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Pearsons chi-squared test used to test the normality?', 'Pearson,chi-squared,χ2');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Is the Shapiro-Francia test used to test the normality?', 'Shapiro-Francia');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (1, 'Did they plot a histogram/graph and it looked normal distributed?', 'Histogram,plot,normal distribution');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Is the correlation test used to test the linearity?', 'correlation coefficient,significance,Pearson');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Did they mentioned that the relationships are linear?', 'linearity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Are r^2 tests executed?', 'coefficient of determination,R^2,r^2,R2,R squared');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Did they use the anova test of linearity?', 'anova,linearity,linear regression');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Did they used a graphical method to test the linearity?', 'plot,diagram,histogram,diagram,chart,box plot,graph,heatmap,pie chart,plotting,scatterplot,skewplot,sparkline,stemplot,radar chart');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Does the eta correlation ratio shows linearity?', 'eta,correlation ratio,statistical dispersion,standard deviations');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (2, 'Do the plot variables reveals linearity?', 'plot,diagram,histogram,diagram,chart,box plot,graph,heatmap,pie chart,plotting,scatterplot,skewplot,sparkline,stemplot,radar chart,linearity');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (3, 'Is the Box M test used to test the Homogeneity of variance?', 'Box M,Homogeneity,Covariance,Matrices');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Did they use some graphical methods to test the Homoscedasticity (AKA Constant Variance)?', 'plot,histogram,diagram,chart,box plot,graph,heatmap,pie chart,plotting,scatterplot,skewplot,sparkline,stemplot,radar chart');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the WLS regression used to test the Homoscedasticity (AKA Constant Variance)?', 'least square,weighted least square,weighted linear least square,Ω,correlation matrix,residuals');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Goldfeld Quandt test used to test the Homoscedasticity (AKA Constant Variance)?', 'Goldfeld-Quandt,homoscedasticity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Glejser test used to test the Homoscedasticity (AKA Constant Variance)?', 'Glejser,heteroscedasticity,regression,residuals,ordinary least square,sample residuals,R2');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Park test used to test the Homoscedasticity (AKA Constant Variance)?', 'Park,linear regression heteroscedasticity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Breusch Pagan Godfrey test used to test the Homoscedasticity (AKA Constant Variance)?', 'Breusch-Pagan,Godfrey,linear regression,heteroscedasticity,variance,residuals,regression');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the White test used to test the Homoscedasticity (AKA Constant Variance)?', 'White,residual variance,regression,homoscedasticity,heteroscedasticity');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (4, 'Is the Levene test used to test the Homoscedasticity (AKA Constant Variance)?', 'Levene,variance,homogenity of variance,homoscedasticity');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the Fisher exact test used to test the Independence?', 'Fisher,exact test,Fisher-Yates,Fisher-Irwin,chi^2,chi-squared');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the G-test used to test the Independence?', 'G-test,likelihood-ratio,maximum likelihood,statistical significance,chi-squared');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the Hilbert-Schmidt independence criterion used to test the Independence?', 'Hilbert-Schmidt,independence criterion');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (5, 'Is the Schweizer-Wolff approach used to test the Independence?', 'Schweizer-wolff');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (6, 'Are the covariance and the independent variable completely separated?', 'covariance,independent variable');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (7, 'Is the Durbin-Watson test used to test the Independence of error terms?', 'Durbin-watson,autocorrelation,residuals,regression analysis');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (8, 'Are the dependent variables ordinal?', 'ordinal,dependent variable,dependent variables');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (9, 'Does the model include interaction effects?', 'interaction effect,interaction effects');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (10, 'Are the samples greater than 30 (N=30)?', 'sample,population');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (12, 'Are the variables intervals or ratio measurements?', 'intervals,ratio');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (13, 'Are the outliers kept to a minimum or are removed entirely?', 'outliers,minimum,removed');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (14, 'Are the responses ordinal?', 'ordinal');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (15, 'Are the samples of the population random?', 'population,random,samples');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (16, 'Are the data measured at least on an ordinal scale?', 'ordinal');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (17, 'Are the pairs of the dataset chosen randomly and independently?', 'random,independently');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (18, 'Are the continuous distributions the same for all the test variables?', 'continuous,distribution,variables');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (19, 'Do the dataset represent a random sample from the population?', 'random,samples,population');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (20, 'Are the pair in the dataset matched?', 'pair');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (21, 'There exists at least one group in the datset which is measured on three or more different occasions?', 'measured,occasions');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (22, 'Is the dataset randomly sampled?', 'random,sample,population');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (23, 'Is one of the dependant variable either an ordinal interval or ratio?', 'dependant,variable,ordinal,interval,ratio');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (24, 'Are the data quantitatively described?', 'data,quantitative');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (25, 'Are the observations independent?', 'independent,observation');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (26, 'Is the sample size at least 10?', 'sample,population,size');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (27, 'Is the data in frequency form?', 'frequency');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (28, 'Is no particular distribution assumed for the dataset?', 'distribution,assumption,assumed');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (29, 'Is the saple size greater than 20?', 'sample,size,population');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (30, 'Are the cell frequencies greater than 3?', 'frequency,frequencies,cell,cells');".execute.apply()

            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (31, 'Are the cell frequencies greater than 5?', 'frequency,frequencies,cell,cells');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (32, 'Are the K classes when put together a complete serie?', 'K classes,K class,complete series');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (33, 'Have both distributions the same interval classification and the same number of elements?', 'cell,cell frequency');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (34, 'Has the error a constant variance and is on average equals to 0?', 'average,zero,0,error,constant variance');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (35, 'Is there no association between the factor and the measured error?', 'factor,association,measurement,error');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (36, 'Is there no association between the errors?', 'error,errors,association');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (37, 'Do the samples represent the whole population?', 'sample,samples,population,populations');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (38, 'Are the variables not correlated?', 'correlation,correlated');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (39, 'Is the dataset non-informative censored?', 'non-informative,censored');".execute.apply()
            sql"INSERT INTO assumption2questions(assumption_id, question, test_names) values (40, 'Are the hazards proportional?', 'hazards,hazard,proportional');".execute.apply()
          }
          logger.debug("Table Stat_methods created!")
      }
    }
  }
}