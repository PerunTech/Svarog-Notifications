import { React, connect, PropTypes, GenericGrid, createHashHistory, axios, ComponentManager } from 'perun-core'
import InboxMessages from './InboxMessages';
import style from './style/MessagesHolder.module.css'
import ReactPaginate from 'react-paginate';

const hashHistory = createHashHistory()

const tableName = 'SUBJECT'

class InboxComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      objIdState: '',
      objectTypeState: '',
      showGrid: false,
      gridId: `${tableName}_INBOX_GRID`,
      list: [],
      perPage: 10,
      page: 0,
      pageCount: 0,
      totalNumber: 0,
      hideTmp: true
    };
  }

  componentDidMount() {
    this.getTotalNumberPages();
    this.showInboxGrid(0, 10);
  }

  getTotalNumberPages = async () => {
    const { svSession } = this.props
    const { perPage } = this.state
    const url = window.server + `/svarog_notifications/services/countTotalNumberOfObjectsPerCategory/${svSession}/INBOX`
    try {
      const response = await axios.get(url)
      if (response.data) {
        const pageCount = Math.ceil(response.data / perPage)
        this.setState({ totalNumber: response.data, pageCount })
      }
    } catch (err) {
      console.log(err);
    }
  }

  componentWillUnmount() {
    ComponentManager.cleanComponentReducerState(this.state.gridId)
  }


  showInboxGrid = (start, end) => {
    const { gridId } = this.state
    let gridElementArr = []
    const htmlElement = <div className={style['context-menu-holder']}>
      <p className={style['inbox-paragraph']}>Inbox</p>
    </div>
    const grid = <GenericGrid
      gridType={'READ_URL'}
      key={gridId}
      id={gridId}
      configTableName={'/svarog_notifications/services/getTableFieldList/%session/' + tableName}
      dataTableName={`/svarog_notifications/services/getInboxSubjectsWithPagination/%session/${start}/${end}`}
      minHeight={500}
      onRowClickFunct={this.onInboxRowClick}
      customClassName={'customGridClass'}
    />
    gridElementArr.push(htmlElement, grid)
    ComponentManager.cleanComponentReducerState(gridId)

    this.setState({ generateGridElement: gridElementArr, showGrid: false }, () => this.setState({ showGrid: true }))
  }


  onInboxRowClick = (id, idx, row) => {
    const { gridId } = this.state
    const objId = row[`${tableName}.OBJECT_ID`]
    const objectType = row[`${tableName}.OBJECT_TYPE`]
    ComponentManager.cleanComponentReducerState(gridId)
    this.setState({ objIdState: objId, objectTypeState: objectType, showGrid: false, hideTmp: false })
  }

  handleBack = () => {
    this.setState({ objIdState: '', objectTypeState: '', showGrid: true, hideTmp: true })
  }

  handlePageClick = (e) => {
    debugger
    const { perPage, totalNumber } = this.state
    const page = e.selected;
    const start = (page * perPage) % totalNumber;
    const end = start + perPage;
    // this.setState({ page })
    this.showInboxGrid(start, end)
  };


  render() {
    const { generateGridElement, objIdState, objectTypeState, showGrid, pageCount } = this.state
    return (
      <React.Fragment>
        {objIdState && objectTypeState && <InboxMessages handleBack={this.handleBack} objId={objIdState} objType={objectTypeState} />}
        <div>
          {showGrid && generateGridElement}
        </div>
        {this.state.hideTmp && <div id='pagination' className='paginationHolder'>
          <ReactPaginate
            key={'pageCount'}
            breakLabel='...'
            nextLabel={'next'}
            onPageChange={this.handlePageClick}
            pageRangeDisplayed={5}
            containerClassName={'pagination'}
            pageCount={pageCount}
            previousLabel={'prev'}
            activeClassName={'active'}
            renderOnZeroPageCount={null} />
        </div>}
      </React.Fragment>
    )
  }
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

InboxComponent.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(InboxComponent)