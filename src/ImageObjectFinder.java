import java.util.ArrayList;

import java.io.IOException;
import java.io.File;

@SuppressWarnings("serial")
class ExecException extends Exception {
	public ExecException (String msg) {
		super (msg);
	}
}

@SuppressWarnings("serial")
class FileDescException extends Exception {
	public FileDescException (String msg) {
		super ("file/directory error occurs (" + msg + ")");
	}
}

@SuppressWarnings("unchecked")
public class ImageObjectFinder {
	public enum executionType {
		TRAIN_ONLY (0, "train_only"),
		EVAL_ONLY (1, "evaluation_only"),
		TRAIN_W_EVAL (3, "evaluation with additional train"),
		//CLASSIFY (4, "classifying a image"),
		NONE (-1, "nothing to be set");
		
		int id;
		String explain;

		executionType (int id, String explain) {
			this.id = id;
			this.explain = explain;
		}
		public int getId () {
			return this.id;
		}
		public String getExplain () {
			return this.explain;
		}
	}


	private String trainDataPath		= "./train";
	private String validDataPath		= "./valid";
	private String cpDataPath				= "./checkpoint";
	private String resultDataPath		= "./result";
	private String inceptionPath		= "./class";
	private ArrayList<String> cmd		= null;
	private boolean fine						= false;
	private executionType type			= executionType.NONE;
	private int num_gpus						= 0;
	private int batch_size					= 32;
	private int max_steps						= 1000000;

	final
	private String[] merge (final ArrayList<String> ...arrayLists) {
		int size = 0;


		for (ArrayList<String> arrayList : arrayLists)
			size += arrayList.size ();
		
		String[] ret = new String[size];

		int destPos = 0;
		for (int i = 0; i < arrayLists.length; i ++) {
			if (i > 0)
				destPos += arrayLists[i - 1].size ();
			int length = arrayLists[i].size ();
			System.arraycopy (arrayLists[i].toArray (ret), 0, ret, destPos, length);
		}

		return ret;
	}

	public ImageObjectFinder (executionType type) {
		this.type = type;
	}

	public ImageObjectFinder () {
		this (executionType.NONE);
	}

	public void setInceptionPath (String inceptionPath) {
		this.inceptionPath = inceptionPath;
	}

	public void setType (executionType type) {
		this.type = type;
	}

	public String getType () {
		return this.type.getExplain ();
	}

	public void setNumGpus (int num_gpus) {
		this.num_gpus = num_gpus;
	}

	public int getNumGpus () {
		return this.num_gpus;
	}

	public void setBatchSize (int batch_size) {
		this.batch_size = batch_size;
	}

	public int getBatchSize () {
		return this.batch_size;
	}

	public void setMaxSteps (int max_steps) {
		this.max_steps = max_steps;
	}

	public int getMaxSteps () {
		return this.max_steps;
	}

	public void setTrainingPaths (String trainDataPath, String cpDataPath) throws FileDescException {
		{
			File f = null;
			f = new File (trainDataPath);
			if (f.exists () && !f.isDirectory ()) {
				throw new FileDescException (trainDataPath);
			}
		}
		this.trainDataPath = trainDataPath;
		this.cpDataPath = cpDataPath;
	}

	public void setEvaluationPaths (String validDataPath, String cpDataPath, String resultDataPath) throws FileDescException {
		{
			File f = null;
			f = new File (validDataPath);
			if (f.exists () && !f.isDirectory ()) {
				throw new FileDescException (validDataPath);
			}

			f = new File (cpDataPath);
			if (f.exists () && !f.isDirectory ()) {
				throw new FileDescException (cpDataPath);
			}
		}
		this.validDataPath = validDataPath;
		this.cpDataPath = cpDataPath;
		this.resultDataPath = resultDataPath;
	}

	public void setDataPaths (String trainDataPath,
			String validDataPath,
			String cpDataPath,
			String resultDataPath) throws FileDescException {
		setTrainingPaths (trainDataPath, null);
		setEvaluationPaths (validDataPath, cpDataPath, resultDataPath);
	}

	public void run () throws ExecException, IOException {
		run (this.max_steps, this.batch_size, this.num_gpus, this.fine);
	}

	public void run (int max_steps) throws ExecException, IOException {
		run (max_steps, this.batch_size, this.num_gpus, this.fine);
	}

	public void run (int max_steps, int batch_size) throws ExecException, IOException {
		run (max_steps, batch_size, this.num_gpus, this.fine);
	}

	public void run (int max_steps, int batch_size, int num_gpus) throws ExecException, IOException {
		run (max_steps, batch_size, num_gpus, this.fine);
	}

	public void run (int max_steps, int batch_size, int num_gpus, boolean fine) throws ExecException, IOException {
		Runtime rt = Runtime.getRuntime ();
		ArrayList<String> cmds = new ArrayList<String> ();
		ArrayList<String> opts = new ArrayList<String> ();

		cmds.add (this.inceptionPath);
		cmds.add ("--batch_size=" + this.batch_size);
		cmds.add ("--max_steps=" + this.max_steps);

		if (this.type == executionType.TRAIN_ONLY) {
			opts.add ("--data_dir " + this.trainDataPath);
			opts.add ("--train_dir " + this.cpDataPath);
			if (this.fine == true) {
				opts.add ("--fine_tune True");
			}
			if (num_gpus != 0) {
				opts.add ("--num_gpus=" + this.num_gpus);
			}
			rt.exec (this.merge (cmds, opts));
		}
		else if (this.type == executionType.EVAL_ONLY) {
			opts.add ("--data_dir " + this.validDataPath);
			opts.add ("--checkpoint_dir " + this.cpDataPath);
			opts.add ("--eval_dir " + this.resultDataPath);
			opts.add ("--run_once");
			rt.exec (this.merge (cmds, opts));
			//rt.exec ((String[]) ArrayUtils.addAll ((String[]) cmds.toArray (), (String[]) opts.toArray ()));
		}
		else if (this.type == executionType.TRAIN_W_EVAL) {
			opts.add ("--data_dir " + this.trainDataPath);
			opts.add ("--train_dir " + this.cpDataPath);
			if (this.fine == true) {
				opts.add ("--fine_tune True");
			}
			if (num_gpus != 0) {
				opts.add ("--num_gpus=" + this.num_gpus);
			}
			rt.exec (this.merge (cmds, opts));
			//rt.exec ((String[]) ArrayUtils.addAll ((String[]) cmds.toArray (), (String[]) opts.toArray ()));

			opts.clear ();
			opts.add ("--data_dir " + this.validDataPath);
			opts.add ("--checkpoint_dir " + this.cpDataPath);
			opts.add ("--eval_dir " + this.resultDataPath);
			rt.exec (this.merge (cmds, opts));
			//rt.exec ((String[]) ArrayUtils.addAll ((String[]) cmds.toArray (), (String[]) opts.toArray ()));
		}
		/*else if (this.type == CLASSIFY) {
		}*/
		else {
			throw new ExecException ("unknown command \"" + this.type.getExplain () + "\"");
		}
	}

}
