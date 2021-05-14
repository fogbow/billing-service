package cloud.fogbow.fs.core.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;

@Entity
@Table(name = "volume_item_table")
public class VolumeItem extends ResourceItem {
	public static final String ITEM_TYPE_NAME = "volume";

    private static final String SIZE_COLUMN_SIZE = "size";
	
    @Column(name = SIZE_COLUMN_SIZE)
	private int size;

    public VolumeItem() {
        
    }
    
	public VolumeItem(int size) throws InvalidParameterException {
		setSize(size);
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) throws InvalidParameterException {
		if (size < 0) {
			throw new InvalidParameterException(Messages.Exception.NEGATIVE_VOLUME_SIZE);
		}
		
		this.size = size;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + size;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VolumeItem other = (VolumeItem) obj;
		if (size != other.size)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "{\"type\":\"volume\", \"size\":" + size + "}";
	}
}
