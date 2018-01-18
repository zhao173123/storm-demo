*********************************Storm简介***********************************************************************
Storm是一个实时计算框架，没有提供存储功能。可以考虑postgresql、mysql、hbase等持久化存储。
🌟注意：类的详解基本都在countword包中🌟
------------------------------------------------------------------------------------------
1.Storm集群中，有两类节点：主节点Master node和工作节点Worker nodes。
	主节点运行Nimbus守护进程，这个进程负责在集群中分发代码，为工作节点分配任务，并监控故障。
	Supervisor守护进程作为拓扑的一部分运行在工作节点上。
2.Storm有两种模式，本地模式和远程模式。本地模式运行在本地的单一jvm上，这个模式用于开发、测试以及调试。
	note:本地模式和远程模式相似，但是要关注所有组件都是线程安全的，因为当把它们部署到远程模式时，它们可能会运行
		在不同的虚拟机间，这时候它们之间没有直接的通讯或共享内存。
	在远程模式下，我们向Storm集群提交拓扑，它通常由许多运行在不同机器上的流程组成。远程模式不会出现调试信息，
	因此也被成为生产模式。

*****************************************************************************************************************

********************************Storm理解*************************************************************************
Storm分布式计算集群中任务称为Topology，仅由spout、bolt组成，其中spout采集数据，bolt处理数据。

Spout：Storm Topology的主要数据入口，充当采集器的角色，连接到数据源，将数据转化为N个Tuple，并将这些Tuple进行发射；
Bolt：计算单元，接收（订阅）N个Tuple数据流（Spout或者其他Bolt发射）作为输入，对数据实施运算后，选择性的输出一个或多个数据流；
Tuple：Stream的最小组成单元，每次Spout发送给Bolt的数据称之为Tuple，Stream流就是这些源源不断的Tuple组成的。
	   Storm的核心是Tuple，Tuple是包含了一个或多个键值对的列表，Stream由无限制的Tuple组成的序列
Task：Storm集群中每个Spout和Bolt都由若干task来执行，每个task都与一个执行线程相对应。
Stream：Spout从外部获取数据，以一定的格式传递给Bolt处理。从Spout源源不断给Bolt传递数据，形成的数据通道称之为Stream。
现在Stream有8种stream group：
	1.Shuffle Grouping：随机分组，随机派发stream里面的tuple，保证每个bolt接收到的tuple数目相同。
	    				    使得每个任务所处理的Tuple数量能够保持一致，以确保集群的负载均衡；
	2.Fields Grouping：域分组，具有同样的key的tuple会被分到相同的Bolts，不同key的tuple分配到不同的bolt。
	    				   例如：如果某个数据流是基于一个名为“user-id”的域进行分组的，那么所有包含相同的“user-id”的Tuple都会被
	    				   分配到同一个任务中，确保消息处理的一致性；
	3.Partial Key Grouping：部分关键字分组，和Fields Grouping相似，根据定义的域来对数据流进行分组，不同的是，这种方式会考虑下游
								bolt数据处理的均衡性问题，在输入数据源关键字不平衡时会有更好性能；
	4.All Grouping：完全分组（广播派发），数据流会被发送到Bolt的所有任务中（同一个Tuple被复制多份然后被所有的任务处理），
	    				使用这种方式需要特别小心；
	5.Global Grouping：全局分组，所有的数据流都被发送到Bolt的同一个任务中，也就是id最小的task；
	6.None Grouping：不分组，不关心数据流如何分组，目前和使用Shuffle Grouping完全等效；
	7.Direct Grouping：直接分组，消息的发送者可以指定下游的哪个任务接收这个Tuple。
	      				  只有被声明为Direct Stream的消息流可以声明这种分组方法。而且这种消息tuple必须使用emitDirect来发射消息，
	      				  消息处理者可以通过TopologyContext来处理它的消息taskId
	8.Local or shufle grouping:本地域随机分组，如果在源组件的 worker 进程里目标 Bolt 有一个或更多的任务线程，
								   元组会被随机分配到那些同进程的任务中。换句话说，这与随机分组的方式具有相似的效果。
Workers：工作进程，Tuple（拓扑）是在一个或多个工作进程（work process）中运行的。每个工作进程都是一个实际的JVM，并且
		执行拓扑的一个子集。例如：如果拓扑的并行度定义为300，工作进程定义为50，那么每个工作进程就会执行6个任务（进程内部的线程）。
		Storm会在所有的worker中分散任务，以便实现集群的负载均衡。
		Config.TOPOLOGY_WORKERS这个配置项用于设置拓扑的工作进程。
		
Worker、Executor、Task关系：
1.先参考worker.png
2.在worker中运行的是拓扑的一个子集。一个worker进程是从属于某一个特定拓扑的，在worker进程中会运行一个或多个与拓扑中组件相关联的executor（执行线程）。
一个运行中的拓扑就是由这些运行于Storm集群中的很多机器上的worker进程组成的。
一个executor是由worker进程生成的一个线程。在executor中可能会有一个或多个task，这些task都是为同一个组件（spout或bolt）服务的。
task是实际执行数据处理的最小工作单元（task不是线程），在整个拓扑的生命周期中每个组件的task数量都是保持不变的。
*********************************************************************************************************************		
数据路由：一个Topology可能有多个Spout从外部数据源获取数据，某些场景下会希望同一条数据被spoutA、spoutB获取；
而另一些场景下，只被spoutC获取。如果数据源不支持路由，意味着需要开发不同的获取机制，无疑会加大开发成本。
现有的JMS消息机制（kafka、RabbitMQ等）消息中间件刚好满足数据路由的条件。
所以一般Storm的数据源都是MQ。
*********************************************************************************************************************
---------------------------------------------------------------------------------------------------------------------
图片example-of-a-running-topology.png解析：

图中是一个包含有两个 worker 进程的拓扑。其中蓝色的 BlueSpout 有两个 executor，每个 executor 中有一个 task，并行度为 2；
绿色的 GreenBolt 有两个 executor，每个 executor 有两个 task，并行度也为2；
而黄色的YellowBolt 有 6 个 executor，每个 executor 中有一个 task，并行度为 6。
因此，这个拓扑的总并行度就是 2 + 2 + 6 = 10。具体分配到每个 worker 就有 10 / 2 = 5 个 executor。

相关代码：
Config conf = new Config();
conf.setNumWorkers(2);//use two worker processes
topologyBuilder.setSpout("blue-spout", new BlueSpout(), 2);//set parallelism hint to 2
topologyBuilder.setBolt("green-bolt", new GreenBolt(), 2)
				.setNumTasks(4)
				.shuffleGrouping("blue-bolt");
topologyBuilder.setBolt("yellow-bolt", new YellowBolt(), 6)
				.shuffleGrouping("green-bolt");
StormSubmitter.submitTopology("mytopology", conf, topologyBuilder.createTopology());

PS:
Storm可以随时增加或减少worker、executor数量，而不需要重启集群或者拓扑，这个方式叫做再平衡。
//重新配置拓扑"mytopology",使得该拓扑拥有5个worker processes
//配置名为"blue-spout"的spout使用3个executor
//配置名为"yellow-bolt"的bolt使用10个executor
$storm rebalance mytopology -n 5 -e blue-spout=3 -e yellow-bolt=10
---------------------------------------------------------------------------------------------------------------------
*********************************************************************************************************************
🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟
Trident是Storm的最核心的概念，绝大部分情况下我们都会使用Trident，而不是传统的Spout、Bolt。
Trident是Storm原语的封装，使得我们开发Storm变得更方便和简单。可以将Trident和Storm Topology的关系类比ORM和JDBC的关系。
Trident对Storm原语的抽象针对Spout、Bolt、Grouping等基本概念，主要体现在Trident Spout、Operation、State。
Trident是在Storm基础上，一个以realtime计算为目的的高度抽象实时计算模型。
Trident提供低延时分布式查询（DRPC）和有状态（实时保存最新的聚合信息，重启无影响）流式处理能力。
Trident的功能：join（连接）、aggregation（聚合）、分组（grouping）、函数（function）、过滤器（filter）

State:管理状态
错误无法消除，所在在节点出现故障或者其他问题发生时，批处理操作需要进行重试。这里的问题在于执行哪一种合适的
状态更新操作（不管是针对外部数据还是拓扑内部的状态），来使得每个消息都能够执行且仅仅执行一次。
Trident的解决方案：
1.在Trident中为每个数据块标记了一个唯一的id，这个id就叫做“事物id”，如果数据块由于失败而回滚，那么它持有的事物id不会改变；
2.State的更新操作是按照数据块的顺序进行的。在成功执行完块2的更新操作之前，不会执行块3的更新操作。
基于这2个基础特性，state更新就可以实现恰好一次的语义。
Trident Spout:对原语中Spout的抽象
Operation:针对Bolt、Grouping等的抽象
Fields&Tuple：
Trident的数据模型TridentTuple是一个指定的指列表。在一个拓扑中，tuple是在一系列操作中不断生成的。这些操作一般会有一个“输入源”（input fields）集合，
然后发送一个方法域（Function fields）集合，方法域主要用于为该操作的输出结果域命名。

Trident中使用Trident Topology表示一个拓扑，而Storm中使用Storm Topology来表示拓扑。

StormTopology和TridentTopology的区别：
一：API区别
Storm Topology:由传统的Spout、Bolt来编写，最终都通过TopologyBuilder对象来创建。
	TopologyBuilder builder = new TopologyBuilder();
	StormTopology topology = builder.createTopology();
TridentTopology:由Trident的API来编写，直接new
	TridentTopology trientTopology = new TridentTopology();
二：创建Topology区别
StormTopology:
	builder.setSpout("word-reader", new WordReader(), 4);
	builder.setBolt("word-normalizer", new WordNormalizer(), 3)
			.shuffleGrouping("word-reader");
	builder.setBolt("word-counter", new WordCount(), 1)
			.fieldsGrouping("word-normalizer", new Fields("word"));
	StormTopology topology = builder.createTopology();
TridentTopology:
	tridentTopology.newStream("word-reader-stream", new WordReader())//SpoutNode
					.parallelismHint(16)
					.each(new Fields("line"), new NormalizeFunction(), new Fields("word"))//processerNode
					.groupBy(new Fields("word"))//PartitionNode
					.persistentAggregate(new MemoryMapState.Factory(), new Sum(), new Fields("sum"));//processerNode
	StormTopology stormTopology = tridentTopology.build();//这里看出，TridentTopology最终还是会编译成StormTopology

---------------------------------------------------------------------------------------------------------------------
Trident的核心数据模型是stream,stream是作为一系列数据块来处理。数据流是分布在集群中的不同节点上运行的，并且对数据流的操作也是在数据流的每个小分区上并行进行。
🌟Trident有5类操作：
1⃣️本地分区操作：在每个分区块上独立运行的操作，不涉及网络数据传输
1.函数 function：负责接收一个输入域的集合并选择输出或者不输出Tuple。
	输出Tuple的域会被添加到原始数据流的输入域中；如果不输出Tuple，那么原始的输入Tuple就会被直接过滤掉。
	实例：
	假设有一个mystream的数据流，这个流包含几个tuple，每个tuple包含a，b，c三个域：
	[1,2,3]
	[4,1,6]
	[3,0,8]
	public class MyFunction extends BaseFunction{
		public void execute(TridentTuple tuple, TridentCollector collector){
			for(int i = 0; i < tuple.getInteger(0); i++){
				collector.emit(new Values(i));
			}
		}
	}
	运行如下代码：
	mystream.each(new Fields("b"), new MyFunction(), new Fields("d"));
	那么最终会输出的结果tuple就会包含"a","b","c","d"4个域，结果如下：
	[1,2,3,0]
	[1,2,3,1]
	[4,1,6,0]

2.过滤器 filter：负责输入的Tuple是否需要保留
	实例：
	假设有一个stream数据流，包含几个tuple，tuple包含"a","b","c"三个域，形如：
	[1,2,3]
	[2,1,1]
	[2,3,4]
	public class MyFilter extends BaseFilter{
		public boolean isKeep(TridentTuple tuple){
			return tuple.getInteger(0) == 1 && tuple.getInteger(1) == 2;
		}
	}
	运行如下代码：
	mystream.each(new Fields("b", "a"), new MyFilter());
	执行结果如下：
	[2,1,1]

3.聚合 partitionAggregate：会在一批Tuple的每个分区上执行一个指定的动作。由partitionAggregate发送出的Tuple会将输入的Tuple域替换掉。
	实例：
	myStream.partitionAggregate(new Fields("b"), new Sum(), new Fields("sum"));
	假如输入流中包含“a”、“b”两个域并且几个tuple块
	Partition 0:["a",1] ["b",2]
	Partition 1:["a",3] ["c",4]
	Partition 2:["e",1] ["d",9] ["d",10]
	执行后，输出会带有一个名为“sum”域的数据流，其中tuple格式Partition 0:[3] Partition 1:[7] Partition 2:[20]
	
	Storm有3个用于定义聚合器的接口，CombinerAggregator|ReducerAggregator|Aggregator
	3.1 CombinerAggregator
	将带有一个域的单独的tuple返回作为输出，会在每个输出tuple上运行初始化函数，然后使用组合函数来组合所有输入的值。
	如果某个分区没有tuple，CombinerAggregator就会输出zero方法的结果。
	public interface CombinerAggregator<T> extends Serializable{
		T init(TridentTuple tuple);
		T combine(T val1, T val2);
		T zero();
	}
	基于CombinerAggregator实现的Count：
	public class Count implements CombinerAggregator<Long>{
		public Long init(TridentTuple tuple){
			return 1L;
		}
		public Long combine(Long val1, Long val2){
			return val1 + val2;
		}
		public Long zero(){
			return 0L;
		}
	}
	3.2	ReducerAggregator
	使用init来产生一个初始化的值，然后使用该值对每个输入tuple进行遍历，并最终生成输出一个单独的tuple，这个tuple就包含我们需要计算的结果值。
	public interface ReducerAggregator<T> extends Serializable{
		T init();
		T reduce(T curr, TridentTuple tuple);
	}
	基于ReducerAggregator实现Count：
	public class Count implements ReducerAggregator<Long>{
		public Long init(){
			return 0L;
		}
		public Long reduce(Long curr, TridentTuple tuple){
			return curr + 1;
		}		
	}
	3.3 Aggregator
		最常用的聚合接口，可以生成任意数量的tuple，这些tuple也可以带任意数量的域。
		聚合器可以在执行过程中的任意一点输出tuple，执行过程如下：
		3.3.1 在处理一批数据之前先调用init()，init()的返回值是一个代表着聚合状态的对象，这个对象接下来会被传入aggregate()方法和complete()方法
		3.3.2 对一个区块中的每个tuple都会调用aggregate()，更新状态并且有选择的输出tuple
		3.3.3 在区块中的所有tuple都被aggregate()方法处理之后就会调用complete()
	基于Aggregator实现的Count:
	public class Count implements Aggregator<CountState>{
		static class CountState{
			long count = 0;
		}
		public CountState init(Object batchId, TridentCollector collector){
			return new CountState();
		}
		public void aggregate(CountState state, TridentTuple tuple, TridentCollector collector){
			state.count += 1;
		}
		public void complete(CountState state, TridentCollector collector){
			collector.emit(new Values(state.count));
		}
	}
	有时你会执行多个聚合操作，这个过程叫做链式处理，如下：
	mystream.chainAgg()
			.partitionAggregate(new Count(), new Fields("count"))
			.partitionAggregate(new Fields("b"), new Sum(), new Fields("sum"))
			.chainEnd();
	以上代码会在每个分区上分别执行Count和Sum聚合器，而输出只会包含一个带有["count", "sum"]域的单独tuple。
	
4.stateQuery和partitionPersist
	stateQuery查询state数据
	partitionPersist更新state数据
	
5.projection（投影）
	只会保留操作中指定的域，如果有一个带有["a", "b", "c", "d"]域的数据流，执行
	mystream.project(new Fields("b", "d"));
	输出结果：["b", "d"]
	
2⃣️重分区操作
	重分区操作会执行一个用来改变在不同的任务间分配tuple的方式的函数。在重分区的过程中分区的数量也可能发生变化，
	重分区会产生一定的网络传输。包含一下几个函数：
	2.1 shuffle():通过随机轮询算法来重新分配目标区块的所有tuple，均衡分配。
	2.2 broadcast():每个tuple都会被复制到所有的目标区块中。在DPRPC中很有用，可以获取每个区块数据的查询结果。
		DRPC:分布式RPC，Storm接收若函数参数作为输入流，然后通过DRPC输出这些函数调用的结果。
			 DRPC是一个通过DRPC服务端来实现分布式RPC功能的，DRPC server负责接收RPC请求，并将该请求发送到Storm运行的Topology上，
			 等待接收Topology发送的处理结果，并将该结果返回给发送请求的客户端。
			 原理示意图参见：images/drpc-workflow.png
			 客户端通过向DRPC服务器发送待执行函数的名称以及该函数的参数来获取处理结果。
			 实现该函数的拓扑使用一个DRPCSpout从DRPC服务器中接收一个函数调用流。DRPC会为每个函数调用都标记唯一的id，随后拓扑会执行函数计算结果，
			 并在拓扑的最后使用一个名为ReturnResults的bolt来连接到DRPC服务器，根据函数调用的id来将函数调用的结果返回。
	2.3 partitionBy():接收一组域作为参数，并根据这些来进行分区。可以通过对这些域进行哈希并对目标分区的数量取模的方法来选取目标区块。
					  能够保证来自同一组域的结果总会被发送到相同的目标区间。
	2.4 global():所有的tuple都会被发送到同一个目标分区中，而且数据流中所有的块都会由这个分区处理。
	2.5 batchGlobal():同一个batch块中的tuple会被发送到同一个区块中。
	2.6 partition():使用自定义的分区方法

3⃣️聚类操作
	Trident使用aggregate和persistentAggregate方法来对数据流进行聚类操作。
	其中，aggregate会分别对数据流中的每个batch进行处理，而persistentAggregate则对数据流中所有batch进行聚类处理，并将结果存入某个state中。
	在数据流上进行aggregate操作会执行一个全局的聚类操作。
	在使用ReducerAggregator或Aggregator时，数据流会被重新分区成一个单独的分区，然后聚类函数就会在该分区上执行操作；
	而使用CombinerAggregator时，Trident首先会计算每个分区的部分聚类结果，然后将这些结果重分区到一个单独的分区中，最后在网络传输完成之后结束这个聚类过程。
	因此，CombinerAggregator比其他聚类器的运行效率要高，在聚类时应该尽量使用CombinerAggregator。
	实例：使用aggregate来获取一个batch的全局技术值
	mystream.aggregate(new Count(), new Fields("count"));
	和partitionAggregate一样，aggregate的聚合器也可以进行链式处理。然而，如果在一个处理链中同时使用了CombinerAggregator和非CombinerAggregator，
	Trident就不能对部分聚类进行优化了。

4⃣️对分组数据流的操作
	通过对指定域执行partitionBy或groupBy操作可以对数据流进行重新分区，使得相同域的tuple分组可以聚集在一起。
	操作实例参见group.png

5⃣️融合merge、联结join
	最简单的就是将所有数据流都融合到一个数据流中，如：
	topology.merge(stream1, stream2, stream3);
	Trident会将融合后的新数据流的域命名为第一个数据流的输出域。
	Trident的join只会应用于每个从spout中输出的小batch
	实例：两个流的join操作，其中一个流含有["key", "val1", "val2"]域，另外一个含有["x", "val1"]域
	topolpogy.join(stream1, new Fields("key"), stream2, new Fields("x"), new Fields("key", "a", "b", "c"));
	使用"key"和"x"作为join的域来联结stream1和stream2，结果：
	["key", "stream1.val1", "stream1.val2", "stream2.val1"]
来源：https://github.com/weyo/Storm-Documents/blob/master/Manual/zh/Trident-API-Overview.md		
--------------------------------------------------------------------------------------------------------------------
🌟️Trident Spout
	Trident拓扑中大部分的spout都是非事务性的，所有spout都必须有唯一的标识，而且这个标识必须在整个storm集群中都是唯一的。
	Trident需要使用这个标识来存储spout从zk中消费的元数据（metadata），包括txid以及其他相关的spout元数据。
	
	Storm对ZK集群的配置：
	1.transactional.zookeeper.servers：ZooKeeper 的服务器列表
	2.transactional.zookeeper.port：ZooKeeper 集群的端口
	3.transactional.zookeeper.root：元数据在 ZooKeeper 中存储的根目录。元数据会直接存储在该设置目录下。

	常用的spout API接口：
	1.ITridentSpout：这是最常用的 API，支持事务型和模糊事务型的语义实现。不过一般会根据需要使用它的某个已有的实现，而不是直接实现该接口。
	2.IBatchSpout：非事务型 spout，每次会输出一个 batch 的 tuple。
	3.IPartitionedTridentSpout：可以从分布式数据源（比如一个集群或者 Kafka 服务器）读取数据的事务型 spout。
	4.OpaquePartitionedTridentSpout：可以从分布式数据源读取数据的模糊事务型 spout。
	
	*******************************************************************************************************************
	🏁Trident是通过小数据块（batch）的方式来处理tuple的，而且每个batch都会有唯一个txid。
	有三种支持容错性的Spout，分别是"非事务型(non-transactional)","事物型(transactional)","模糊事物型(opaque transactional)"
	1.Transactional Spouts （事物型Spout）
	事物型Spout特性：
		1.1 每个batch的txid永远不会变，对于某个特定的txid，batch在执行重新操作时所处理的tuple集和它的第一次操作完全相同；
		1.2 不同batch中的tuple不会出现重复的情况，某个tuple只会出现在batch中，而不会同时出现在多个btach中；
		1.3 每个tuple都会被放入一个batch中，处理操作不会遗漏任何tuple
	实例：
	假设正在处理txid3，包含以下tuple ：["man"] ["man"] ["dog"]
	假如数据库中已经存在key-value对：
	man => [count = 3, txid = 1]
	dog => [count = 4, txid = 3]
	apple => [count = 10, txid = 2]
	其中于"man"相关联的txid为1。由于当前处理的txid为3，就可以确定当前处理的batch与数据库中存储的值无关，
	结果：
	man => [count = 5, txid = 3]
	dog => [count = 4, txid = 3]
	apple => [count = 10, txid = 2]
	2.Opaque Transactional Spouts（模糊事物型）
	特性：每个tuple都会通过某个batch处理完成。不过，在tuple处理失败的时候，tuple有可能继续在另一个batch中完成处理，而不一定是原先的batch中。
	模糊事物型spout具有最好的容错性，缺点就是不能通过txid来识别数据库的state是否已经被处理了。
	这种情况下，必须在数据库中存储更多state的信息，除了一个txid和结果值外，还应该存入前一个结果值preValue。
	实例：
	假设batch的部分计数值为"2",现在需要应用一个更新操作，
	假定现在数据库中的值是这样
	{
		value = 4,
		preValue = 1,
		txid = 2
	}
	情形1:假如当前处理的txid=3，这与数据库中的txid不同。这时，可以将"preValue"的值设为"value"，再为"value"的值加上部分计数的结果并更新txid，
	{
		value = 6,
		preValue = 4,
		txid = 3
	}
	情形2:如果当前处理的txid=2，也就是和数据库中的txid相同，此时你应该知道数据库的更新操作是由上一个拥有相同txid的batch处理的，
	不过这个batch和当前的batch并不相同，所以需要忽略它(其他batch的操作)的操作。这时，应该将"preValue"加上batch的部分计数值来计算新的value，
	{
		value = 3,
		preValue = 1,
		txid = 2
	}
	3.Non-transactional Spouts
	不能为batch提供任何的安全保证，non-transactional有可能提供一种"至多一次"的处理模型，这种情况下batch处理失败后tuple并不会重新处理。
	也有可能提供"至少一次"的处理模型，在这种情况下会有多个batch分别处理某个tuple。
	
State API：
基本的state api接口只有两个方法
	public interface State{
		void beginCommit(Long txid);
		void commit(Long txid);
	}
❤❤❤️️️实例：
	假如有一个包含有用户的地址信息的数据库，需要使用Trident和该数据库交互，State的实现就会包含用于获取和设置用户信息的方法
	public class LocationDB implements State{
		public void beginCommit(Long txid){}
		public void commit(Long txid){}
		public void setLocationBulk(List<Long> userIds, List<String> locations){
			//code to access database and set location
		}
		public String getBulkLocations(List<Long> userIds){
			//code to get location from database
		}
	}
	接着，就可以为Trident提供一个StateFactory来创建Trident任务内部的State对象的实例，
	public class LocationDBFactory implements StateFactory{
		public State makeState(Map conf, int partitionIndex, int numPartitions){
			return new LocationDB();
		}
	}
	Trident提供了一个用于查询state数据源的QueryFunction接口，以及一个用于更新state数据源的StateUpdater接口。
	查询LocationDB中的用户地址信息的“QueryLocation”
	QueryLocation的执行包含2个操作。
	首先，Trident会将读取的一些数据汇总为一个batch传入batchRetrieve。这里batchRetrieve会收到一些用户id
	然后，batchRetrieve会返回一个与输入tuple列表大小相同的队列。
	public class QueryLocation extends BaseFunction<LocationDB, String>{
		public List<String> batchRetrieve(LocationDB state, List<TridentTuple> inputs){
			List<Long> userIds = new ArrayList<Long>();
			for(TridentTuple input : inputs){
				userIds.add(input.getLong(0));
			}
			return state.getBulkLocations(userIds);
		}
		public void execute(TridentTuple tuple, String location, TridentCollector collector){
			collector.emit(new Values(location));
		}
	}
	在拓扑中读取输入流中的userId信息：
	TridentTopology topology = new TridentTopology();
	TridentState location = topology.newStaticState(new LocationDBFactory());
	topology.newStream("myspout", spout)
			.stateQuery(locations, new Fields("userId"), new QueryLocation(), new Fields("location"));
	-----------------------
	需要使用StateUpdater接口来更新state，以下是一个更新LocationDB的地址信息的StateUpdater实现：
	public class LocationUpdater extends BaseStateUpdater{
		public void updateState(LocationDB state, List<TridentTuple> tuples, TridentCollector collector){
			List<Long> ids = new ArrayList<Long>();
			List<String> locations = new ArrayList<String>();
			for(TridentTuple t : tuples){
				ids.add(t.getLong(0));
				locations.add(t.getString(1));
			}
			state.setLocationsBulk(ids, locations);
		}
	}
	然后就可以在Trident拓扑中使用这个操作：
	TridentTopology topology = new TridentTopology();
	TridentState locations = topology.newStream("locations", locationsSpout)
									 .partitionPersist(new LocationDBFactory(), new Fields("userId", "location"), new LocationUpdater());
	partitionPersist操作会更新state数据源。StateUpdater接收State和一批tuple作为输入，然后更新这个state。
	上面的代码从输入tuple中抓取userId和location在信息，然后对state执行一个批处理操作。
	在Trident拓扑更新LocarionDB后，partitionPersist会返回一个更新后状态的TridentState对象。
	随后你就可以在拓扑的其他地方使用stateQuery方法对这个state进行查询操作。

persistentAggregate
	Trident使用persistentAggregate来更新state
	TridentTopology topology = new TridentTopology();
	TridentState wordCounts = 
		topology.newStream("spout1", spout)
				.each(new Fields("sentence"), new Split(), new Fields("word"))
				.groupBy(new Fields("word"))
				.persistentAggregate(new MemoryMapState.Factory(), new Count(), new Fields("count"));
	partitionPersist是一个接收Trident聚合器作为参数并对state数据源进行更新的方法，persistentAggregate就是构建于partitionPersist上层的一个编程抽象。
	这里由于是一个分组数据流（groupBy），Trident需要你提供一个实现MapState接口的state。被分组的域就是state的key，而聚合的结果就是state中的value。
	MapState接口：
	public interface MapState<T> extends State{
		List<T> multiGet(List<List<Object>> keys);
		List<T>	multiUpdate(List<List<Object>> keys, List<ValueUpdater> updaters);
		void multiPut(List<List<Object>> keys, List<T> vals);
	}
	而当你在非分组数据流上进行聚合操作（全局聚合操作），Trident需要你提供一个实现了Snapshottable接口的对象。
	public interface Snapshottable<T> extends State{
		T get();
		T update(ValueUpdater updater);
		void set(T o);
	}
	MemoryMapState和MemcachedState都实现了上面2个接口。
	
Map State接口
	实现MapState接口很简单，Trident几乎为你做好了所有的准备工作。OpaqueMap、TransactionalMap、NonTransactionalMap都分别实现了
	各自的容错性语义。你只需要为这些类提供一个用于对不同的key/value进行multiGet和multiPut处理的IBackingMap实现类。
	IBackMap接口：
	public interface IBackingMap<T>{
		List<T> multiGet(List<List<Ovject>> keys);
		void multiPut(List<List<Object>> keys, List<T> vals);
	}										 
来源：https://github.com/weyo/Storm-Documents/blob/master/Manual/zh/Trident-Spouts.md
来源：https://github.com/weyo/Storm-Documents/blob/master/Manual/zh/Trident-State.md
--------------------------------------------------------------------------------------------------------------------
Storm源码组织结构：
Storm代码有三个层次，
第一，Storm在一开始就是按照兼容多语言的目的来设计的。Nimbus是Thrift服务，拓扑也被定义为Thrift架构。
第二，所有的Storm接口都设计为java接口。所以，尽管Storm核心代码中有大量Clojure实现，所有的访问都必须经过JAVA API。
第三，Storm的实现中大量使用了Clojure。由于Clojure更具有表现力，实际上Storm的核心逻辑大多采用Clojure来实现的。

Java接口
Storm的对外接口基本上为Java接口，主要的接口有
1.IRichBolt
2.IRichSpout
3.TopologyBuilder
大部分的接口策略为1.使用一个java接口来定义接口 2.实现一个具有适当的默认实现的Base类
来源：https://github.com/weyo/Storm-Documents/blob/master/Manual/zh/Structure-Of-The-Codebase.md
以上Storm的理解内容大部分来源：https://github.com/weyo/Storm-Documents/blob/master/Manual/zh/Trident-Tutorial.md
🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟🌟




