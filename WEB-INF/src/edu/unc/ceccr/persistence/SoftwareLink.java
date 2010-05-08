package edu.unc.ceccr.persistence;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

@SuppressWarnings("serial")
@Entity
@Table(name="cbench_softwareLink")
public class SoftwareLink implements java.io.Serializable{
	//For one compound and one descriptor generation program, this class stores all the descriptors.
	//e.g., the string "descriptorValues" could contain the 900 values generated by Dragon for lorazepam.
	
	private Long id;
	private String name; 
	private String function; //"Compound Visualization", "Duplicate Detection", etc.
	private String availability; //Commercial, free for academic, etc.
	private String reference; //literature reference (if any)
	private String url; //link
	private String type; //dataset, modeling, workflows, etc. Separate tables.
	
	public SoftwareLink(){}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "function")
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}

	@Column(name = "availability")
	public String getAvailability() {
		return availability;
	}
	public void setAvailability(String availability) {
		this.availability = availability;
	}

	@Column(name = "reference")
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}

	@Column(name = "url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	@Column(name = "type")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
}
