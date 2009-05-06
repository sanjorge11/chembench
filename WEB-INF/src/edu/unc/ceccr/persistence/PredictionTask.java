package edu.unc.ceccr.persistence;

import java.sql.SQLException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.utilities.Utility;

@SuppressWarnings("serial")
@Entity()
@Table(name = "cbench_prediction_task")
public class PredictionTask implements java.io.Serializable{

	private Long id;
	
	private Long datasetId;	
	
	public PredictionTask(Long id, Long datasetId) {
		this.datasetId = datasetId;
		this.id = id;
	}

	/**
	 * @return the id
	 */
	@Id
	@Column(name="id")
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the datasetId
	 */
	@Column(name="datasetID")
	public Long getDatasetId() {
		return datasetId;
	}

	/**
	 * @param datasetId the datasetId to set
	 */
	public void setDatasetId(Long datasetId) {
		this.datasetId = datasetId;
	}
	public void save(PredictionTask t) throws HibernateException, ClassNotFoundException, SQLException {
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = s.beginTransaction();
			s.saveOrUpdate(t);
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}
			
	}
	
}
